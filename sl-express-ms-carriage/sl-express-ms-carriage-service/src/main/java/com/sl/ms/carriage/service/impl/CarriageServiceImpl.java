package com.sl.ms.carriage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sl.ms.carriage.domain.dto.CarriageDTO;
import com.sl.ms.carriage.domain.dto.WaybillDTO;
import com.sl.ms.carriage.entity.CarriageEntity;
import com.sl.ms.carriage.mapper.CarriageMapper;
import com.sl.ms.carriage.service.CarriageService;
import com.sl.ms.carriage.utils.CarriageUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CarriageServiceImpl extends ServiceImpl<CarriageMapper, CarriageEntity>
        implements CarriageService {

    @Override
    public CarriageDTO saveOrUpdate(CarriageDTO carriageDto) {
        return null;
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
        return null;
    }

    @Override
    public CarriageEntity findByTemplateType(Integer templateType) {
//        if (ObjectUtil.equals(templateType, CarriageConstant.ECONOMIC_ZONE)) {
//            throw new SLException(CarriageExceptionEnum.METHOD_CALL_ERROR);
//        }
//        LambdaQueryWrapper<CarriageEntity> queryWrapper = Wrappers.lambdaQuery(CarriageEntity.class)
//                .eq(CarriageEntity::getTemplateType, templateType)
//                .eq(CarriageEntity::getTransportType, CarriageConstant.REGULAR_FAST);
//        return super.getOne(queryWrapper);
        return null;
    }
}