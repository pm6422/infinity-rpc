package org.infinity.luix.demoserver.service.impl;

import org.infinity.luix.core.server.annotation.RpcProvider;
import org.infinity.luix.democommon.domain.AdminMenu;
import org.infinity.luix.democommon.dto.AdminMenuTreeDTO;
import org.infinity.luix.democommon.service.AdminMenuService;
import org.infinity.luix.demoserver.repository.AdminMenuRepository;
import org.thymeleaf.util.StringUtils;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RpcProvider
public class AdminMenuServiceImpl implements AdminMenuService {

    @Resource
    private AdminMenuRepository adminMenuRepository;

    @Override
    public List<AdminMenuTreeDTO> getMenus() {
        List<AdminMenu> adminMenus = adminMenuRepository.findAll();
        return generateTree(adminMenus);
    }

    private List<AdminMenuTreeDTO> generateTree(List<AdminMenu> menus) {
        // 根节点
        List<AdminMenuTreeDTO> rootMenus = menus.stream()
                .filter(menu -> StringUtils.isEmpty(menu.getParentId()))
                .map(AdminMenu::toTreeDTO)
                .sorted(Comparator.comparing(AdminMenuTreeDTO::getSequence))
                .collect(Collectors.toList());
        rootMenus.forEach(rootMenu -> {
            // 给根节点设置子节点
            rootMenu.setChildren(getChildren(rootMenu.getId(), menus));
        });
        return rootMenus;
    }

    private List<AdminMenuTreeDTO> getChildren(String parentId, List<AdminMenu> menus) {
        // 子菜单
        List<AdminMenuTreeDTO> childMenus = menus.stream()
                .filter(menu -> parentId.equals(menu.getParentId()))
                .map(AdminMenu::toTreeDTO)
                .sorted(Comparator.comparing(AdminMenuTreeDTO::getSequence))
                .collect(Collectors.toList());
        // 递归
        for (AdminMenuTreeDTO childMenu : childMenus) {
            childMenu.setChildren(getChildren(childMenu.getId(), menus));
        }
        return childMenus;
    }
}