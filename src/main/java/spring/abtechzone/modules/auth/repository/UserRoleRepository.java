package spring.abtechzone.modules.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import spring.abtechzone.modules.auth.entity.UserRole;
import spring.abtechzone.modules.auth.entity.UserRoleId;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {}
