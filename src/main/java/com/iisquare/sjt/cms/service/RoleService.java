package com.iisquare.sjt.cms.service;

import com.iisquare.sjt.cms.core.ServiceBase;
import com.iisquare.sjt.cms.dao.RelationDao;
import com.iisquare.sjt.cms.dao.RoleDao;
import com.iisquare.sjt.cms.domain.Relation;
import com.iisquare.sjt.cms.domain.Role;
import com.iisquare.sjt.cms.utils.DPUtil;
import com.iisquare.sjt.cms.utils.ServiceUtil;
import com.iisquare.sjt.cms.utils.ValidateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;

@Service
public class RoleService extends ServiceBase {

    @Autowired
    private RoleDao roleDao;
    @Autowired
    private UserService userService;
    @Autowired
    private RelationDao relationDao;

    public Set<Integer> relationIds(String type, Integer aid, Set<Integer> bids) {
        type = "role_" + type;
        List<Relation> list = relationDao.findAllByTypeAndAid(type, aid);
        if(null == bids) {
            return ServiceUtil.getPropertyValues(list, Integer.class, "bid");
        } else {
            relationDao.deleteAll(list);
            list = new ArrayList<>();
            for (Integer bid : bids) {
                list.add(Relation.builder().id(type + "_" + aid + "_" + bid).type(type).aid(aid).bid(bid).build());
            }
            relationDao.saveAll(list);
            return bids;
        }
    }

    public Map<?, ?> status(String level) {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "正常");
        status.put(2, "禁用");
        switch (level) {
            case "default":
                break;
            case "full":
                status.put(-1, "已删除");
                break;
            default:
                return null;
        }
        return status;
    }

    public Role info(Integer id) {
        if(null == id || id < 1) return null;
        return roleDao.findById(id).get();
    }

    public Role save(Role info, int uid) {
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        return roleDao.save(info);
    }

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<?> data = roleDao.findAll(new Specification() {
            @Override
            public Predicate toPredicate(Root root, CriteriaQuery query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.notEqual(root.get("status"), -1));
                String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
                if(!DPUtil.empty(name)) {
                    predicates.add(cb.like(root.get("name"), "%" + name + "%"));
                }
                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            }
        }, PageRequest.of(page - 1, pageSize, Sort.by(Sort.Order.desc("sort"))));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            userService.fillInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withStatusText"))) {
            ServiceUtil.fillProperties(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public boolean remove(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        roleDao.deleteInBatch(roleDao.findAllById(ids));
        return true;
    }

    public boolean delete(List<Integer> ids) {
        if(null == ids || ids.size() < 1) return false;
        List<Role> list = roleDao.findAllById(ids);
        for (Role item : list) {
            item.setStatus(-1);
        }
        roleDao.saveAll(list);
        return true;
    }

}
