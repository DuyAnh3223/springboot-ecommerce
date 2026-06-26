package spring.abtechzone.modules.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import spring.abtechzone.modules.auth.entity.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {}
