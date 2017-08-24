package org.mitre.tangerine.netowl.parser;

import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.parser.Parser;

import java.io.InputStream;
import java.util.logging.Level;
import java.io.FileOutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Marshaller;

public class NetOwlParser extends Parser<Content> {

	public void marshall(Content sourceFile, FileOutputStream os) throws AETException {
		try {
			JAXBContext jc = JAXBContext.newInstance("org.mitre.tangerine.netowl.parser");
			Marshaller m2 = jc.createMarshaller();
			m2.marshal(sourceFile, os);
		} catch (JAXBException je) {
			throw new AETException(Level.SEVERE, je.getMessage(), 1);
		}
	}

	@Override
	public Content parse(InputStream is) throws AETException {
		Content mcElement = null;

		try {
			JAXBContext jc = JAXBContext.newInstance("org.mitre.tangerine.netowl.parser");
			Unmarshaller u = jc.createUnmarshaller();
			mcElement = (Content) u.unmarshal(is);
		} catch (JAXBException je) {
			throw new AETException(Level.SEVERE, je.getMessage(), 2);
		}

		return (mcElement);
	}
}
