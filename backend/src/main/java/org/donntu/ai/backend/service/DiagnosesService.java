package org.donntu.ai.backend.service;

import org.donntu.ai.backend.dto.DiagnosisBySymptomsResponse;
import org.donntu.ai.backend.entity.Diagnosis;
import org.donntu.ai.backend.entity.Symptom;
import org.donntu.ai.backend.repository.DiagnosesRepository;
import org.donntu.ai.backend.repository.SymptomsRepository;
import org.donntu.ai.backend.utils.SetsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DiagnosesService {
    private final DiagnosesRepository diagnosesRepository;
    private final SymptomsRepository symptomsRepository;

    private Set<Diagnosis> diagnoses = new HashSet<>();
    private Set<Symptom> symptoms = new HashSet<>();

    @Autowired
    public DiagnosesService(DiagnosesRepository diagnosesRepository, SymptomsRepository symptomsRepository) {
        this.diagnosesRepository = diagnosesRepository;
        this.symptomsRepository = symptomsRepository;

        diagnoses.addAll(SetsUtils.iterableToSet(diagnosesRepository.findAll()));
        symptoms.addAll(SetsUtils.iterableToSet(symptomsRepository.findAll()));
    }

    public DiagnosisBySymptomsResponse getDiagnosesBySymptoms(Set<Symptom> symptoms) {
        Set<Diagnosis> diagnosesList = new HashSet<>();
        diagnoses.forEach(diagnosis -> {
            if (isDiagnosisContainsAllSymptoms(diagnosis, symptoms)) {
                diagnosesList.add(diagnosis);
            }
        });
        return new DiagnosisBySymptomsResponse(diagnosesList);
    }

    private boolean isDiagnosisContainsAllSymptoms(Diagnosis diagnosis, Set<Symptom> symptoms) {
        Set<Symptom> diagnosisSymptoms = diagnosis.getSymptoms();
        for (Symptom symptom : symptoms) {
            if (!diagnosisSymptoms.contains(symptom)) {
                return false;
            }
        }
        return true;
    }

    private boolean isHaveSymptomsInput(Diagnosis source, Diagnosis dest) {
        if (source.getSymptoms().size() > dest.getSymptoms().size()) {
            return isDiagnosisContainsAllSymptoms(source, dest.getSymptoms());
        } else {
            return isDiagnosisContainsAllSymptoms(dest, source.getSymptoms());
        }
    }

    private boolean isAnyDiagnosisHaveSymptomsInput(Diagnosis diagnosis) {
        return diagnoses
                .stream()
                .filter(value -> !value.equals(diagnosis))
                .anyMatch(value -> isHaveSymptomsInput(value, diagnosis));
    }

    private boolean isAnyDiagnosisHaveSymptomsInput(Diagnosis diagnosis, Set<Diagnosis> diagnoses) {
        return diagnoses
                .stream()
                .anyMatch(value -> isHaveSymptomsInput(value, diagnosis));
    }

    //приходит то без id
    public boolean addSymptom(Symptom symptom) {
        if (!symptoms.contains(symptom)) {
            Symptom saved = symptomsRepository.save(symptom);
            symptoms.add(saved);
            return true;
        } else {
            return false;
        }
    }

    public boolean updateSymptom(Symptom symptom) throws Exception {
        Optional<Symptom> symptomById = getSymptomById(symptom.getId());
        if (symptomById.isPresent()) {
            Symptom saved = symptomsRepository.save(symptom);
            symptoms.remove(symptomById.get());
            return symptoms.add(saved);
        } else {
            throw new Exception("Такого симптома не существует");
        }
    }

    public boolean deleteSymptom(Symptom symptom) throws Exception {
        if(symptoms.contains(symptom)) {
            Set<Diagnosis> diagnosisContainsThisSymptom = diagnoses
                    .stream()
                    .filter(diagnosis -> diagnosis.getSymptoms().contains(symptom))
                    .map(diagnosis -> {
                        HashSet<Symptom> symptoms = new HashSet<>(diagnosis.getSymptoms());
                        symptoms.remove(symptom);
                        return new Diagnosis(diagnosis.getId(), diagnosis.getName(), symptoms);
                    }).collect(Collectors.toSet());
            for (Diagnosis diagnosis : diagnosisContainsThisSymptom) {
                if(isAnyDiagnosisHaveSymptomsInput(diagnosis)) {
                    return false;
                }
            }
            symptomsRepository.delete(symptom);
            symptoms.remove(symptom);
            diagnosisContainsThisSymptom.forEach(diagnosis -> diagnosis.getSymptoms().remove(symptom));
            return true;
        } else {
            throw new Exception("Такого симптома не существует");
        }
    }

    public boolean addDiagnosis(Diagnosis diagnosis) {
        if (isAnyDiagnosisHaveSymptomsInput(diagnosis)) {
            return false;
        } else {
            Diagnosis saved = diagnosesRepository.save(diagnosis);
            diagnoses.add(saved);
            return true;
        }
    }

    public boolean updateDiagnosis(Diagnosis diagnosis) throws Exception {
        Optional<Diagnosis> byId = getDiagnosisById(diagnosis.getId());
        if (byId.isPresent()) {
            Set<Diagnosis> compareList = new HashSet<>(diagnoses);
            compareList.remove(byId.get());
            if (isAnyDiagnosisHaveSymptomsInput(diagnosis, compareList)) {
                return false;
            } else {
                Diagnosis saved = diagnosesRepository.save(diagnosis);
                diagnoses.remove(byId.get());
                diagnoses.add(saved);
                return true;
            }
        } else {
            throw new Exception("Такого диагноза не существует");
        }
    }

    public boolean deleteDiagnosis(Long id) throws Exception {
        Optional<Diagnosis> byId = getDiagnosisById(id);
        if (byId.isPresent()) {
            diagnosesRepository.delete(byId.get());
            return diagnoses.remove(byId.get());
        } else {
            throw new Exception("Такого диагноза не существует");
        }
    }

    private Optional<Diagnosis> getDiagnosisById(Long id) {
        return diagnoses
                .stream()
                .filter(diagnosis -> diagnosis.getId().equals(id))
                .findFirst();
    }

    private Optional<Symptom> getSymptomById(Long id) {
        return symptoms
                .stream()
                .filter(symptom -> symptom.getId().equals(id))
                .findFirst();
    }
}