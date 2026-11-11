package com.chroniclesoftime.planning.infrastructure.persistence;

import com.chroniclesoftime.planning.domain.model.AdmissionSimulation;
import com.chroniclesoftime.planning.domain.repository.AdmissionSimulationRepository;
import com.chroniclesoftime.shared.BaseMapperRepository;
import org.springframework.stereotype.Repository;

@Repository
public class AdmissionSimulationRepositoryImpl extends BaseMapperRepository<AdmissionSimulation, AdmissionSimulationMapper> implements AdmissionSimulationRepository {
    public AdmissionSimulationRepositoryImpl(AdmissionSimulationMapper mapper) { super(mapper); }
}
