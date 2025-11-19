package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private DishMapper dishMapper;


    @Override
    //@CacheEvict(value = "shoppingCart", key = "#shoppingCartDTO.userId")
    public void add(ShoppingCartDTO shoppingCartDTO) {
        //属性拷贝
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);//拷贝菜品id，套餐id
        shoppingCart.setCreateTime(LocalDateTime.now());
        shoppingCart.setUserId(BaseContext.getCurrentId());

    /*    //判断是菜品还是套餐
        if (shoppingCart.getDishId() != null) {
            //判断购物车中此菜品是否已存在
            Long count = shoppingCartMapper.countByDishId(shoppingCart);
            String flover =shoppingCartDTO.getDishFlavor();
            //判断购物车中此菜品的口味是否已存在
            int isExistFlover = 0;
            List<String> flovors = dishFlavorMapper.getNamesByDishId(shoppingCart.getDishId());
            if (flovors.contains(flover)) {
                isExistFlover = 1;
            }
            if (count == 0|| isExistFlover == 1) {
            shoppingCart.setDishFlavor(shoppingCartDTO.getDishFlavor());
            shoppingCart.setNumber(1);
            }
            else {
                shoppingCart.setNumber((int) (count+1));
            }
            Dish dish = dishMapper.getById(shoppingCart.getDishId());
            shoppingCart.setName(dish.getName());
            shoppingCart.setImage(dish.getImage());
            shoppingCart.setAmount(dish.getPrice());
            shoppingCartMapper.add(shoppingCart);

        }
        else if (shoppingCart.getSetmealId() != null) {
            //判断购物车中是否已存在此套餐
            Long  count = shoppingCartMapper.countBySetmealId(shoppingCart);

            Setmeal setmeal = setmealMapper.getById(shoppingCart.getSetmealId());//获取套餐
            shoppingCart.setName(setmeal.getName());
            shoppingCart.setImage(setmeal.getImage());
            shoppingCart.setAmount(setmeal.getPrice());
            if (count == 0) {
                shoppingCart.setNumber(1);
            }
            else {
                shoppingCart.setNumber((int) (count+1));
            }
                shoppingCartMapper.add(shoppingCart);

        }*/
        //判断是否存在购物车
        List<ShoppingCart> list = shoppingCartMapper.selectIsExist(shoppingCart);

        //存在则修改
        if (list !=null && list.size() > 0 ) {
            ShoppingCart existing = list.get(0);
            existing.setNumber(existing.getNumber() + 1);
            shoppingCartMapper.update(existing);
        }

        //不存在则添加
        else {
            //菜品
            if (shoppingCart.getDishId() != null) {
                Dish dish = dishMapper.getById(shoppingCart.getDishId());
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setDishFlavor(shoppingCartDTO.getDishFlavor());

            }
            //套餐
            else {
                Setmeal setmeal = setmealMapper.getById(shoppingCart.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCartMapper.add(shoppingCart);

        }
    }
    @Override
    //@Cacheable(value = "shoppingCart", key = "#userId")
    public List<ShoppingCart> list() {
        return  shoppingCartMapper.list(BaseContext.getCurrentId());
    }

    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartMapper.selectIsExist(shoppingCart);
        if (list == null || list.size() == 0) {
            return;
        }
        ShoppingCart cart = list.get(0);
        if (cart.getNumber() == 1) {
            shoppingCartMapper.delete(cart);
        }
        else {
            cart.setNumber(cart.getNumber() - 1);
            shoppingCartMapper.update(cart);
        }
    }

    @Override
    public void clean() {
        shoppingCartMapper.clean(BaseContext.getCurrentId());
    }
}
