package org.yuca.diet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.yuca.diet.entity.DietGoal;

/**
 * 用户饮食目标Mapper
 */
@Mapper
public interface DietGoalMapper extends BaseMapper<DietGoal> {
}
