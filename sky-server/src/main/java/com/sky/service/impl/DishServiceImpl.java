package com.sky.service.impl;

import com.sky.anno.AutoFill;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    //新增菜品
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
    //分页查询
    @Override
    @Transactional
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        dishPageQueryDTO.setPage((dishPageQueryDTO.getPage()-1)*dishPageQueryDTO.getPageSize());
        Long total =dishMapper.pageQueryTotal(dishPageQueryDTO);
        List<DishVO> list = dishMapper.pageQueryRecords(dishPageQueryDTO);
        log.info("查询到的菜品数据：{}", list);
        log.info("查询到的总记录数：{}", total);
        //return new PageResult(total, list);
        PageResult pageResult = new PageResult(total, list);
        return pageResult;
    }
    //批量删除菜品
    @Override
    @Transactional
    public void delete(List<Long> ids) {
        Dish dish = new Dish();
        for(Long id: ids){
            dish=dishMapper.getById(id);
            //如果菜品是起售状态，不能删除
            if(dish.getStatus() == 1){
                throw new RuntimeException("起售中的菜品不能删除");}
            //如果套餐里有这个菜品，不能删除
            if(dishMapper.getSetmealIdByDishId(id)>0){
                throw new RuntimeException("套餐中包含此菜品，不能删除");}
            //删除菜品基本信息
            dishMapper.delete(id);
            //删除菜品口味
            dishMapper.deleteDishFlavor(id);
        }

    }
    //根据id查询菜品
    @Override
    @Transactional
    public DishVO getById(Long id) {
        DishVO dishVO = new DishVO();
        Dish dish = dishMapper.getById(id);
        //拷贝属性
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishMapper.getDishFlavorById(id));
        return dishVO;
    }
    //修改菜品
    @Override
    @Transactional
    public void update(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);
        dishMapper.deleteDishFlavor(dishDTO.getId());
        List<DishFlavor> dishFlavor = dishDTO.getFlavors();
        for(DishFlavor flavor: dishFlavor){
            flavor.setDishId(dish.getId());
        }
        dishMapper.insertDishFlavor(dishDTO.getFlavors());
    }


    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
/*
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
*/

}
