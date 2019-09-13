package uk.gov.wildfyre.gpcadaptor.dao;


import ca.uhn.fhir.model.dstu2.composite.NarrativeDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Composition;
import ca.uhn.fhir.model.dstu2.resource.Parameters;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Immunization;
import org.hl7.fhir.dstu3.model.Reference;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.wildfyre.gpcadaptor.support.StructuredRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class ImmunizationDao implements IImmunization {

    private static final Logger log = LoggerFactory.getLogger(ImmunizationDao.class);

    SimpleDateFormat
            format = new SimpleDateFormat("dd-MMM-yyyy");


    @Override
    public List<Immunization> search(IGenericClient client, ReferenceParam patient)  {



        String sectionCode="IMM";
        if (patient == null) {
            return Collections.emptyList();
        }

        Parameters parameters  = StructuredRecord.getUnStructuredRecordParameters(patient.getValue(),sectionCode);

        Bundle result = null;
        try {
            result = client.operation().onType(Patient.class)
                    .named("$gpc.getcarerecord")
                    .withParameters(parameters)
                    .returnResourceType(Bundle.class)
                    .encodedJson()
                    .execute();
        } catch (Exception ignore) {

        }

        return processResult(result,sectionCode,patient);
    }

    private List<Immunization> processResult(Bundle result, String sectionCode, ReferenceParam patient) {
        List<Immunization> immunizations = new ArrayList<>();
        if (result != null) {
            for (Bundle.Entry entry : result.getEntry()) {
                if (entry.getResource() instanceof Composition) {

                    Composition doc = (Composition) entry.getResource();

                    for (Composition.Section
                            section : doc.getSection()) {
                        if (section.getCode().getCodingFirstRep().getCode().equals(sectionCode)) {
                            log.info("Processing Section IMM");
                            immunizations = extractImmunizations(section, patient);
                        }
                    }
                }
            }

        }
        return immunizations;
    }

    private List<Immunization> extractImmunizations(Composition.Section section,ReferenceParam patient) {
        List<Immunization> immunizations = new ArrayList<>();

        NarrativeDt text = section.getText();

        Document doc = Jsoup.parse(text.getDivAsString());
        org.jsoup.select.Elements rows = doc.select("tr");
        boolean problems = false;
        int h=1;
        for(org.jsoup.nodes.Element row :rows)
        {
            org.jsoup.select.Elements columns =row.select("th");
            for (org.jsoup.nodes.Element column:columns)
            {

                if (column.text().equals("Details")) {
                    problems = true;
                } else {
                    problems = false;
                }

            }
             processColumns( row, problems, patient, h, immunizations);
             h++;

        }
        return immunizations;
    }

    private void processColumns(
                                org.jsoup.nodes.Element row,
                                boolean problems,
                                ReferenceParam patient,
                                int h,
                                List<Immunization> immunizations) {

        if (problems) {
            org.jsoup.select.Elements columns = row.select("td");
            Immunization immunization = new Immunization();
            immunization.setId("#"+h);
            immunization.setPatient(new Reference
                    ("Patient/"+patient.getIdPart()));
            Immunization.ImmunizationVaccinationProtocolComponent vaccination = immunization.addVaccinationProtocol();

            int g = 0;
            for (org.jsoup.nodes.Element column : columns) {

                processCols(immunization, column, g, vaccination);
                g++;
            }
            if (immunization.hasVaccineCode() )
                immunizations.add(immunization);
        }

    }
    private void processCols(Immunization immunization,
                             org.jsoup.nodes.Element column,
                             int g,
                             Immunization.ImmunizationVaccinationProtocolComponent vaccination){
        if (g==0) {
            try {
                Date date = format.parse ( column.text() );
                immunization.setDate(date);
            }
            catch (Exception ignore) {
                // No action
            }
        }
        if (g==1) {
            vaccination.setDescription(column.text());
        }
        if (g==2) {
            try {
                int seq = Integer.parseInt(column.text());
                if (seq > 0) {
                    vaccination.setDoseSequence(seq);
                }
            } catch (Exception ignore) {
                // No action
            }

        }
        if (g==3) {
            CodeableConcept code = new CodeableConcept();
            code.setText(column.text());
            immunization.setVaccineCode(code);
        }

    }

}


