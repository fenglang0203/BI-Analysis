package com.sxit.mgt.system.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sxit.common.action.BaseAction;
import com.sxit.common.annatation.AuthPassport;
import com.sxit.common.dto.ResultMessage;
import com.sxit.common.dto.SearchVo;
import com.sxit.common.excel.ExcelExport;
import com.sxit.common.excel.ExcelUtil;
import com.sxit.common.pagehelper.Page;
import com.sxit.common.pagehelper.PageVo;
import com.sxit.common.security.MD5;
import com.sxit.common.utils.MyBeanUtils;
import com.sxit.mgt.system.dto.SysUserModel;
import com.sxit.mgt.system.service.BOrgService;
import com.sxit.mgt.system.service.SysUserService;
import com.sxit.model.common.CheckBoxStringVo;
import com.sxit.model.common.CheckBoxVo;
import com.sxit.model.common.IntegerMap;
import com.sxit.model.system.BOrg;
import com.sxit.model.system.BaseUser;
import com.sxit.model.system.SysUser;

/**
 * @公司:深讯信科
 * @功能:系统用户 Action
 * @作者:张如兵
 * @日期:2015-06-26 09:43:07
 * @版本:1.0
 * @修改:
 */

@Controller
@RequestMapping("/system")
public class SysUserAction extends BaseAction {

	@Autowired
	private SysUserService sysUserService;
	
	@Autowired
	private BOrgService bOrgService;


	@AuthPassport(rightCode = "sysUser_manage")
	@RequestMapping(value = "/sysUserManage")
	public String manage() {
		return "system/sysUser/manage";
	}

	/**
	 * 列表
	 * 
	 * @param searchTxt
	 * @param page
	 * @param model
	 * @return
	 */
	@AuthPassport(rightCode = "System.SysUser")
	@RequestMapping(value = "/sysUserList")
	public @ResponseBody
	ResultMessage list(@ModelAttribute SearchVo vo, PageVo pagevo) {

		// 列表查询
		if (pagevo == null) {
			pagevo = new PageVo(0, 10);
		}

		String orgId = vo.getOrgId();

		if (orgId == null) {
			return ResultMessage.successPage(new Page());
		}

		BOrg org = bOrgService.getBOrgById(orgId);

		if (org == null) {
			return ResultMessage.successPage(new Page());
		}

		Map map = vo.getMap();

		map.put("path", org.getPath());

		Page page = sysUserService.getSysUserList(pagevo, map);

		return ResultMessage.successPage(page);
	}

	@AuthPassport(rightCode = "system.sysuser")
	@RequestMapping(value = "/sysUserExport")
	public @ResponseBody
	ResultMessage export(@ModelAttribute SearchVo vo) {
		PageVo pagevo = new PageVo(0, 5000);
		List list = sysUserService.getSysUserList(pagevo, vo.getMap());

		if (list != null && list.size() > 0) {
			Map map = new HashMap();

			Map<Integer, String> stateMap = new HashMap<Integer, String>();
			stateMap.put(0, "禁用");
			stateMap.put(1, "正常");
			stateMap.put(2, "冻结");
			stateMap.put(3, "删除");
			map.put("stateMap", stateMap);

			Map<Integer, String> userTypeMap = new HashMap<Integer, String>();
			userTypeMap.put(1, "集团管理员");
			userTypeMap.put(2, "区域管理员");
			userTypeMap.put(3, "地市公司管理员");
			userTypeMap.put(4, "员工");


			map.put("userTypeMap", userTypeMap);

			try {
				ExcelExport export = ExcelUtil.exportList(list, "userExport",
						"用户数据", map);
				if(export!=null)
				{
					this.dowloadExcel(export, "userdata.xls");
					return ResultMessage.successMsg("下载成功!");
				}else{
					return ResultMessage.successMsg("下载失败!");
				}

			} catch (Exception e) {
				e.printStackTrace();

				return ResultMessage.errorMsg("下载出错!");
			}
		} else {
			return ResultMessage.errorMsg("您要下载的数据为空!");
		}

	}

	@AuthPassport(rightCode = "System.SysUser")
	@RequestMapping(value = "/sysRoleList")
	public @ResponseBody
	ResultMessage roleList(@ModelAttribute SearchVo vo) {
		BaseUser user = this.getCurUser();

		Map map = new HashMap();
		map.put("orgId", user.getOrgId());

		List<CheckBoxVo> sysRoleList = sysUserService
				.getRoleCheckboxListByMap(map);

		return ResultMessage.successMsg("success", sysRoleList);
	}

	@AuthPassport(rightCode = "System.SysUser")
	@RequestMapping(value = "/sysUserTypeList")
	public @ResponseBody
	ResultMessage userTypeList(@ModelAttribute SearchVo vo) {
		BaseUser user = this.getCurUser();

		IntegerMap im1 = new IntegerMap(1, "集团管理员");
		IntegerMap im2 = new IntegerMap(2, "区域管理员");
		IntegerMap im3 = new IntegerMap(3, "地市管理员");
		IntegerMap im4 = new IntegerMap(4, "员工");
		// IntegerMap

		List<IntegerMap> sysUserTypeList = new ArrayList<IntegerMap>();

		if (user.getUserType() == 1) {
			sysUserTypeList.add(im1);
			sysUserTypeList.add(im2);
			sysUserTypeList.add(im3);
		}
		
		if (user.getUserType() == 99) {
			sysUserTypeList.add(im1);
			sysUserTypeList.add(im2);
			sysUserTypeList.add(im3);
		}

		if (user.getUserType() == 2) {
			sysUserTypeList.add(im2);
			sysUserTypeList.add(im3);
		}

		if (user.getUserType() == 3) {
			sysUserTypeList.add(im3);
		}

		sysUserTypeList.add(im4);

		return ResultMessage.successMsg("success", sysUserTypeList);
	}

	/**
	 * 明细
	 * 
	 * @param userId
	 * @return
	 */
	@AuthPassport(rightCode = "System.SysUser")
	@RequestMapping(value = "/sysUserDetail")
	public @ResponseBody
	ResultMessage detail(@RequestParam Integer userId) {
		String message = "";
		if (userId == null) {
			message = "系统用户ID不能空";
			return ResultMessage.errorMsg(message);
		}

		SysUser sysUser = sysUserService.getSysUserById(userId);
		if (sysUser == null) {
			message = "未找到该系统用户";
			return ResultMessage.errorMsg(message);
		}

		BaseUser user = this.getCurUser();

		Map map = new HashMap();

		map.put("orgId", user.getOrgId());

		map.put("userId", userId);

		List<CheckBoxVo> sysRoleList = null;

		sysRoleList = sysUserService.getRoleCheckboxListByMap(map);

		List<Integer> idList = sysUserService.getRoleIdListByMap(map);

		CheckBoxVo.applyCheckboxList(sysRoleList, idList);
		
		List<CheckBoxVo> checkall = sysUserService.getRoleCheckboxList();
		
		List<Integer> idall = sysUserService.getRoleIdListByUserId(userId);
		
		idall.removeAll(idList);
		
		List<CheckBoxVo> list = CheckBoxVo.applyCheckboxListNoPower(checkall, idall);
		
		sysRoleList.addAll(list);

		// CheckBoxVo
		sysUser.setRoleList(sysRoleList);
		
		//业态
		
		List<CheckBoxStringVo> industList = sysUserService.getIndustryCheckboxListByMap();
		
		List<String> idList2 = sysUserService.getIndustryIdListByUserId(map);
		
		CheckBoxStringVo.applyCheckboxList(industList, idList2);
		sysUser.setIndustryList(industList);

		// 数据权限
		List<String> pidList = sysUserService.getProjectIdList(userId);

		sysUser.setProjGuidList(pidList);

		return ResultMessage.successMsg("获取成功", sysUser);
	}

	/**
	 * 增加
	 * 
	 * @return
	 */
	@AuthPassport(rightCode = "System.SysUser")
	@RequestMapping(value = "/sysUserAdd")
	public @ResponseBody
	ResultMessage add(@Valid @RequestBody SysUserModel sysUserModel,
			Errors errors) {
		// 判断验证是否通过
		if (errors.hasErrors()) {
			StringBuilder sb = new StringBuilder();
			for (FieldError e : errors.getFieldErrors()) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(e.getDefaultMessage());
				break;
			}
			return ResultMessage.errorMsg(sb.toString());
		}

		String password = sysUserModel.getPassword();

		if (password != null && !"".equals(password.trim())) {
			password = MD5.md5(password);
			sysUserModel.setPassword(password.toUpperCase());
		} else {
			sysUserModel.setPassword(null);
		}

		SysUser sysUser = new SysUser();
		BeanUtils.copyProperties(sysUserModel, sysUser);
		sysUser.setCreateTime(new Date());
		// sysUser.setState(1);
		sysUserService.insert(sysUser);
		return ResultMessage.successMsg("添加成功");
	}

	/**
	 * 编辑
	 * 
	 * @param vo
	 * @param userId
	 * @param errors
	 * @return
	 */
	@AuthPassport(rightCode = "System.SysUser")
	@RequestMapping(value = "/sysUserEdit")
	public @ResponseBody
	ResultMessage edit(@Valid @RequestBody SysUserModel sysUserModel,
			Errors errors) {
		// 判断验证是否通过
		if (errors.hasErrors()) {
			StringBuilder sb = new StringBuilder();
			for (FieldError e : errors.getFieldErrors()) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(e.getDefaultMessage());
				break;
			}
			return ResultMessage.errorMsg(sb.toString());
		}

		String password = sysUserModel.getPassword();

		if (password != null && !"".equals(password.trim())) {
			password = MD5.md5(password);
			sysUserModel.setPassword(password.toUpperCase());
		} else {
			sysUserModel.setPassword(null);
		}

		Integer userId = sysUserModel.getUserId();
		String message = "";
		if (userId == null) {
			message = "系统用户ID不能空";
			return ResultMessage.errorMsg(message);
		}

		SysUser sysUser = sysUserService.getSysUserById(userId);
		if (sysUser == null) {
			message = "未找到该系统用户";
			return ResultMessage.errorMsg(message);
		}

		MyBeanUtils.copyProperties(sysUserModel, sysUser, sysUserModel.colset);
		// sysUser.setModifyTime(new Date());

		BaseUser user = this.getCurUser();

		sysUserService.update(sysUser, user);

		return ResultMessage.successMsg("修改成功");
	}

	@AuthPassport(rightCode = "System.SysUser")
	@RequestMapping(value = "/sysUserDataPowerEdit")
	public @ResponseBody
	ResultMessage dataPowerEdit(@Valid @RequestBody SysUserModel sysUserModel,
			Errors errors) {

		// 判断验证是否通过
		if (errors.hasErrors()) {
			StringBuilder sb = new StringBuilder();
			for (FieldError e : errors.getFieldErrors()) {
				if (sb.length() > 0) {
					sb.append(",");
				}
				sb.append(e.getDefaultMessage());
				break;
			}
			return ResultMessage.errorMsg(sb.toString());
		}
		
		BaseUser user = this.getCurUser();
		
		Integer userId = sysUserModel.getUserId();
		String message = "";
		if (userId == null) {
			message = "系统用户ID不能空";
			return ResultMessage.errorMsg(message);
		}
		SysUser sysUser = sysUserService.getSysUserById(userId);
		MyBeanUtils.copyProperties(sysUserModel, sysUser, sysUserModel.colset);
		
		sysUser.setProjGuidList(sysUserModel.getProjGuidList());
		
		
		sysUserService.updateDataPower(sysUser, user);

		return ResultMessage.successMsg("修改成功");

	}

	/**
	 * 
	 * @param userId
	 * @return
	 */
	@AuthPassport(rightCode = "System.SysUser")
	@RequestMapping(value = "/sysUserDelete")
	public @ResponseBody
	ResultMessage delete(@RequestParam Integer userId) {

		if (userId == null) {
			return ResultMessage.errorMsg("系统用户ID不能空");
		}

		SysUser sysUser = sysUserService.getSysUserById(userId);
		if (sysUser == null) {
			return ResultMessage.errorMsg("未找到该系统用户");
		}

		// 判断状态
		// if(sysUser.getState()==2)
		// {
		sysUserService.delete(userId);
		// }else{

		// sysUserService.updateToDelete(userId);
		// }

		return ResultMessage.successMsg("删除成功");
	}

}
