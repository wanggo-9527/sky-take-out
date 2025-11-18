package com.sky.mapper;

import com.sky.anno.AutoFill;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);
    @AutoFill(value = OperationType.INSERT)
    @Insert("INSERT INTO dish (name, category_id, price, image, description, status, create_time, update_time, create_user, update_user) " +
            "VALUES (#{name}, #{categoryId}, #{price}, #{image}, #{description}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void save(Dish dish);

    void insertDishFlavor(List<DishFlavor> flavors);



    Long pageQueryTotal(DishPageQueryDTO dishPageQueryDTO);

    List<DishVO> pageQueryRecords(DishPageQueryDTO dishPageQueryDTO);

    Dish getById(Long id);

    void delete(Long id);

    void deleteDishFlavor(Long id);

    Integer getSetmealIdByDishId(Long id);

    List<DishFlavor> getDishFlavorById(Long id);
    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish);

    /**
     * 根据套餐id查询菜品
     * @param setmealId
     * @return
     */
    @Select("select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = #{setmealId}")
    List<Dish> getBySetmealId(Long setmealId);
}
