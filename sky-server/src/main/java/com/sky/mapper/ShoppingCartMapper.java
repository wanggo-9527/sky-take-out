package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {
    void add(ShoppingCart shoppingCart);



    List<ShoppingCart> selectIsExist(ShoppingCart shoppingCart);

    void update(ShoppingCart shoppingCart);

    List<ShoppingCart> list(Long currentId);

    void sub(ShoppingCart shoppingCart);
    @Delete("delete from shopping_cart where user_id = #{currentId}")
    void clean(Long currentId);
    @Delete("delete from shopping_cart where id = #{id} and user_id=#{userId}")
    void delete(ShoppingCart shoppingCart);
}
