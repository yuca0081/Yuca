package org.yuca.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.yuca.user.entity.RefreshToken;

/**
 * 刷新令牌Mapper接口
 */
@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {
}
