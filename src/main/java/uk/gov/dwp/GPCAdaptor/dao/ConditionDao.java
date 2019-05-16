package uk.gov.dwp.GPCAdaptor.dao;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.dstu2.resource.Bundle;

import ca.uhn.fhir.model.dstu2.resource.Parameters;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hl7.fhir.dstu3.model.Condition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import uk.gov.dwp.GPCAdaptor.support.StructuredRecord;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConditionDao implements ICondition {

    private static final Logger log = LoggerFactory.getLogger(ConditionDao.class);

    @Override
    public List<Condition> search(IGenericClient client, ReferenceParam patient) throws Exception {


        List<Condition> conditions = new ArrayList<>();

        Parameters parameters  = StructuredRecord.getUnStructuredRecordParameters(patient.getValue(),false, false, null);
        FhirContext ctx = FhirContext.forDstu2();
        System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(parameters));

        try {
            Bundle result = client.operation().onType(Patient.class)
                    .named("$gpc.getcarerecord")
                    .withParameters(parameters)
                    .returnResourceType(Bundle.class)
                    .encodedJson()
                    .execute();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();
        }

        return conditions;
    }


}
