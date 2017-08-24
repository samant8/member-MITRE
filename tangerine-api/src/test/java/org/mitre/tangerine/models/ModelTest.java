package org.mitre.tangerine.models;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;
import org.mitre.tangerine.models.AssertionModel;
import org.mitre.tangerine.models.ResponseModel;

import com.google.gson.Gson;

public class ModelTest {

	@Test
	public void testModel() {
		AssertionModel am = new AssertionModel();
		Gson gson = new Gson();
		String subject = "unique_string1", predicate = "unique_string2", object = "unique_string3";

		am.setSPO(subject, predicate, object);
		am.setGeoloc("76", "82");
		am.setGeoloc("76", "82");
		am.setGeoloc("76", "82");
		am.setGeoloc("latitude", "longitude");
		am.setV("id1", "file");
		am.setV("id2", "filex");

		ResponseModel rep = new ResponseModel();
		List<AssertionModel> list = new ArrayList<AssertionModel>();
		list.add(am);
		rep.setAssertions(list);
		String response = gson.toJson(rep);
		assert (response.contains("unique_string1") && response.contains("unique_string2")
				&& response.contains("unique_string3") && response.contains("id2") && response.contains("filex")
				&& response.contains("latitude") && response.contains("longitude"));
	}

	@Test
	public void testModelXML() {
		AssertionModel am = new AssertionModel();
		Gson gson = new Gson();
		String subject = "unique_string1", predicate = "unique_string2", object = "unique_string3";

		am.setSPO(subject, predicate, object);
		am.setGeoloc("76", "82");
		am.setGeoloc("76", "82");
		am.setGeoloc("76", "82");
		am.setGeoloc("latitude", "longitude");
		am.setV("id1", "file");
		am.setV("id2", "filex");

		ResponseModel rep = new ResponseModel();
		List<AssertionModel> list = new ArrayList<AssertionModel>();
		list.add(am);
		rep.setAssertions(list);

		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(ResponseModel.class);

			Marshaller jaxbUnmarshaller = jaxbContext.createMarshaller();
			jaxbUnmarshaller.marshal(rep, System.out);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
