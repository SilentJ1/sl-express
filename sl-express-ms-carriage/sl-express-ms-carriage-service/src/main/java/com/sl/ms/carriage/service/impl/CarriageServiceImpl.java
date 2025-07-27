package com.sl.ms.carriage.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sl.ms.carriage.domain.constant.CarriageConstant;
import com.sl.ms.carriage.domain.dto.CarriageDTO;
import com.sl.ms.carriage.domain.dto.WaybillDTO;
import com.sl.ms.carriage.entity.CarriageEntity;
import com.sl.ms.carriage.enums.CarriageExceptionEnum;
import com.sl.ms.carriage.handler.CarriageChainHandler;
import com.sl.ms.carriage.mapper.CarriageMapper;
import com.sl.ms.carriage.service.CarriageService;
import com.sl.ms.carriage.utils.CarriageUtils;
import com.sl.transport.common.exception.SLException;
import com.sl.transport.common.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CarriageServiceImpl extends ServiceImpl<CarriageMapper, CarriageEntity>
        implements CarriageService {

    @Resource
    private CarriageChainHandler carriageChainHandler;

    @Override
    public CarriageDTO saveOrUpdate(CarriageDTO carriageDto) {
        log.info("新增运费模板 -->", carriageDto);
        // 校验运费模板是否存在，如果不存在直接插入(查询条件: 模板类型 运送类型 如果是修改排除当前id)运输类型
        // 使用LambdaQueryWrapper<>构造查询。模板类型，运送类型相同，且id不同的数据
        LambdaQueryWrapper<CarriageEntity> queryWrapper = new LambdaQueryWrapper<>();
        //eq 方法用于在查询中添加等于条件
        queryWrapper.eq(CarriageEntity::getTemplateType, carriageDto.getTemplateType())
                .eq(CarriageEntity::getTransportType, carriageDto.getTransportType())
                //ne 方法用于添加不等于条件,如果为修改原有数据，则查询id不是carriageDto.getId()的数据
                .ne(ObjectUtil.isNotEmpty(carriageDto.getId()), CarriageEntity::getId, carriageDto.getId());

        //使用LambdaQueryWrapper构造条件后，在数据库中进行查询,不存在直接插入
        List<CarriageEntity> carriageEntityList = super.list(queryWrapper);
        //如果没有重复的模板，可以直接插入或更新操作(DT0 转entity 保存成功 entity 转 DTO)
        //CollUtil.isEmpty判断，而不是 == null判断
        //CollUtil.isEmpty(collection) 更具体地判断集合是否为空集合（即集合对象不为 null 且集合大小为 0）。
        //== null 只能检查对象是否为 null，无法判断对象是否为空集合或者空数组。
        if(CollUtil.isEmpty(carriageEntityList)){
            return saveOrUpdateCarriage(carriageDto);
        }

        //如果存在重复模板，判断此次插入的是否为经济区互寄.ECONOMIC_ZONE常量，非经济区互寄是不可以重复的
        if(ObjectUtil.notEqual(carriageDto.getTemplateType(), CarriageConstant.ECONOMIC_ZONE)){
            //抛出异常：非经济区互寄是不可以重复
            throw new SLException(CarriageExceptionEnum.NOT_ECONOMIC_ZONE_REPEAT);
        }

        //如果是经济区互寄类型，需进一步判断关联城市是否重复，通过集合取交集判断是否重复
        List<String> associatedCityList = carriageEntityList.stream().map(CarriageEntity::getAssociatedCity)
                .map(associatedCity -> StrUtil.splitToArray(associatedCity, ","))
                //防止出现（2，3）的组合，使用flat重新拆散为单个元素
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
        //经济区重复
        Collection<String> intersection = CollUtil.intersection(associatedCityList, carriageDto.getAssociatedCityList());
        if (CollUtil.isNotEmpty(intersection)) {
            throw new SLException(CarriageExceptionEnum.ECONOMIC_ZONE_CITY_REPEAT);
        }

        //如果没有重复，可以新增或更新(DTO转entity 保存成功entity 转DTO)
        return saveOrUpdateCarriage(carriageDto);
    }

    @Override
    public List<CarriageDTO> findAll() {
        //LambdaQueryWrapper<>：这是 MyBatis-Plus 提供的一个查询条件包装器，使用 Lambda 表达式构造查询条件，类型为<>
        LambdaQueryWrapper<CarriageEntity> queryWrapper = new LambdaQueryWrapper();
        //指定排序的字段为 CarriageEntity 实体的 created 字段按降序排序
        queryWrapper.orderByDesc(CarriageEntity::getCreated);
        //调用父类 ServiceImpl 提供的 list 方法
        List<CarriageEntity> list = super.list(queryWrapper);
        return list.stream().map(CarriageUtils::toDTO).collect(Collectors.toList());
        //list.stream()：将 CarriageEntity 实体列表转换为流（Stream），便于后续的流式操作。
        //.map(CarriageUtils::toDTO)：使用 map 方法将 CarriageEntity 实体转换为 CarriageDTO 对象，CarriageUtils::toDTO 是一个方法引用，指向 CarriageUtils 类中的 toDTO 方法。
        //.collect(Collectors.toList())：将转换后的 CarriageDTO 对象收集到一个列表中，并返回该列表。


        //map 是 Java Stream API 中的一个中间操作，它接受一个函数作为参数，并将这个函数应用到流中的每个元素，产生一个新的流。
        //在这里，map 方法将应用于 list 流中的每个 CarriageEntity 元素。转换为 CarriageDTO 对象,产生一个新的流
    }

    @Override
    public CarriageDTO compute(WaybillDTO waybillDTO) {
        //根据参数查找运费模板
        CarriageEntity carriage = this.carriageChainHandler.findCarriage(waybillDTO);

        //计算重量，确保最小重量为1kg
        double computeWeight = this.getComputeWeight(waybillDTO, carriage);

        //计算运费，首重 + 续重
        double expense = carriage.getFirstWeight() + ((computeWeight - 1) * carriage.getContinuousWeight());

        //保留一位小数
        expense = NumberUtil.round(expense, 1).doubleValue();

        //封装运费和计算重量到DTO，并返回
        CarriageDTO carriageDTO = CarriageUtils.toDTO(carriage);
        carriageDTO.setExpense(expense);
        carriageDTO.setComputeWeight(computeWeight);
        return carriageDTO;
    }

    //根据模板，运送类型查找模板
    @Override
    public CarriageEntity findByTemplateType(Integer templateType) {
        LambdaQueryWrapper<CarriageEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CarriageEntity::getTemplateType,templateType);
        //运送模板固定为普快（1）
        queryWrapper.eq(CarriageEntity::getTransportType,CarriageConstant.REGULAR_FAST);
        return super.getOne(queryWrapper);
    }

    @NotNull
    private CarriageDTO saveOrUpdateCarriage(CarriageDTO carriageDto) {
        CarriageEntity carriageEntity = CarriageUtils.toEntity(carriageDto);
        //super为ServiceImpl<CarriageMapper, CarriageEntity>父类
        super.saveOrUpdate(carriageEntity);
        //经过saveOrUpdate后carriageEntity中存入了id（如果为插入），故用转换对象而不是原DTO
        return CarriageUtils.toDTO(carriageEntity);
    }

    /**
     * 根据体积参数与实际重量计算计费重量
     *
     * @param waybillDTO 运费计算对象
     * @param carriage   运费模板
     * @return 计费重量
     */
    private double getComputeWeight(WaybillDTO waybillDTO, CarriageEntity carriage) {
        //计算体积，如果传入体积不需要计算
        Integer volume = waybillDTO.getVolume();
        if (ObjectUtil.isEmpty(volume)) {
            try {
                //长*宽*高计算体积
                volume = waybillDTO.getMeasureLong() * waybillDTO.getMeasureWidth() * waybillDTO.getMeasureHigh();
            } catch (Exception e) {
                //计算出错设置体积为0
                volume = 0;
            }
        }

        // 计算体积重量，体积 / 轻抛系数
        BigDecimal volumeWeight = NumberUtil.div(volume, carriage.getLightThrowingCoefficient(), 1);

        //取大值
        double computeWeight = NumberUtil.max(volumeWeight.doubleValue(), NumberUtil.round(waybillDTO.getWeight(), 1).doubleValue());

        //计算续重，规则：不满1kg，按1kg计费；10kg以下续重以0.1kg计量保留1位小数；10-100kg续重以0.5kg计量保留1位小数；100kg以上四舍五入取整
        if (computeWeight <= 1) {
            return 1;
        }

        if (computeWeight <= 10) {
            return computeWeight;
        }

        // 举例：
        // 108.4kg按照108kg收费
        // 108.5kg按照109kg收费
        // 108.6kg按照109kg收费
        if (computeWeight >= 100) {
            return NumberUtil.round(computeWeight, 0).doubleValue();
        }

        //0.5为一个计算单位，举例：
        // 18.8kg按照19收费，
        // 18.4kg按照18.5kg收费
        // 18.1kg按照18.5kg收费
        // 18.6kg按照19收费
        int integer = NumberUtil.round(computeWeight, 0, RoundingMode.DOWN).intValue();
        if (NumberUtil.sub(computeWeight, integer) == 0) {
            return integer;
        }

        if (NumberUtil.sub(computeWeight, integer) <= 0.5) {
            return NumberUtil.add(integer, 0.5);
        }
        return NumberUtil.add(integer, 1);
    }
}