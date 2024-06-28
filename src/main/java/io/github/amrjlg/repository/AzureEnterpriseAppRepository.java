package io.github.amrjlg.repository;

import io.github.amrjlg.entity.AzureEnterpriseAppManagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author lingjiang
 */
@Repository
public interface AzureEnterpriseAppRepository extends JpaRepository<AzureEnterpriseAppManagement,Long> {

    AzureEnterpriseAppManagement findByAppid(Long appid);
}
