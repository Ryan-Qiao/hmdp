package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.hmdp.service.IFollowService;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Autowired
    IFollowService iFollowService;

    @PutMapping("/{id}/{follow}")
    public Result follow(@PathVariable("id") Long id, @PathVariable("follow") Boolean trueOrNot) {
        return iFollowService.follow(id,trueOrNot);
    }

    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable Long id){
        return iFollowService.isFollow(id);
    }

}
