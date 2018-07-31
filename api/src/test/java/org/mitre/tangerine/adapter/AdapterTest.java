/*
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *                   NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. FA8702-17-C-0001, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (MAY 2013)
 *
 * (c)2016-2017 The MITRE Corporation. All Rights Reserved.
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */


package org.mitre.tangerine.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.junit.Test;
import org.mitre.tangerine.db.AETDatabase;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.ResponseModel;
import org.slf4j.Logger;

public class AdapterTest {

    @Test
    public void failFile() {
        Analytic mock = new FailAdapter();
        try {
            mock.adapt(null, null, null).getErrorMessage().equals("TEST");
        } catch (NullPointerException | IOException e) {
            assert(true);
        } catch (AETException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void updateDB() {
        MockAdapter mock = new MockAdapter();
        try {
            mock.updateDB(null, null);
        } catch (AETException e) {
            assert (e.getMessage().contains("unimplemented"));
        }

    }

    public class MockAdapter extends Analytic {

        public MockAdapter() {
            super();
        }

        @Override
        public void updateDB(ResponseModel data, AETDatabase db) throws AETException {
        }

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getCanonicalName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected Logger getLogger() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getDescription() {
            // TODO Auto-generated method stub
            return null;
        }


        @Override
        public String getDatamap() throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        public Object parse(InputStream is) throws AETException, IOException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ResponseModel adapt(String collection, InputStream data, Map<String, String> params) throws IOException {
            ResponseModel rep = new ResponseModel();
            try (InputStreamReader read = new InputStreamReader(
                    this.getClass().getClassLoader().getResourceAsStream("data-map.json"))) {
            } catch (IOException e) {
                rep.setError(true);
                rep.setErrorMessage("Couldn't open datamap");
            }

            rep.setError(false);
            return rep;
        }



    }

    public class FailAdapter extends Analytic {

        public FailAdapter() {
            super();
            // TODO Auto-generated constructor stub
        }


        @Override
        public void updateDB(ResponseModel data, AETDatabase db) throws AETException {

        }

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getCanonicalName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected Logger getLogger() {
            // TODO Auto-generated method stub
            return null;
        }


        @Override
        public String getDescription() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getDatamap() throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ResponseModel adapt(String collection, InputStream data, Map<String, String> params) throws IOException {
            // TODO Auto-generated method stub
            return null;
        }



    }
}
