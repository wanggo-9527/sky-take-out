package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;


    @Override
    public void save(DishDTO dishDTO) {
        Dish dish = new Dish();
        //拷贝属性
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.save(dish);

        //保存菜品口味
        List<DishFlavor> list = dishDTO.getFlavors();
        for(DishFlavor flavor: list){
            flavor.setDishId(dish.getId());
        }
        dishMapper.insertDishFlavor(dishDTO.getFlavors());

    }
}
