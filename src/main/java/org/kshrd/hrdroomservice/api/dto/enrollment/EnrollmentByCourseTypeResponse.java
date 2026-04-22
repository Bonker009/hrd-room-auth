package org.kshrd.hrdroomservice.api.dto.enrollment;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentByCourseTypeResponse {

    private List<EnrollmentResponse> basic;
    private List<EnrollmentResponse> advanced;
}
