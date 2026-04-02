package com.exam.system.service;

import com.exam.system.dto.common.PageData;
import com.exam.system.dto.version.VersionCompareResponse;
import com.exam.system.dto.version.VersionDetailResponse;
import com.exam.system.dto.version.VersionSummaryResponse;

public interface VersionService {

    PageData<VersionSummaryResponse> listVersions(String entityType, Long entityId, int page, int size);

    VersionDetailResponse getVersion(Long versionId);

    VersionCompareResponse compare(Long versionA, Long versionB);

    void restore(Long versionId);
}
