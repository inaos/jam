package com.inaos.iamj.utility;

import com.inaos.iamj.observation.Observation;
import org.junit.Test;

import java.io.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class SampleUtilityTest {

    @Test
    public void testFileProcessing() throws Exception {
        File sample = File.createTempFile("inaos_sample", ".tmp");

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(sample));
        try {
            out.writeObject(new Observation("met", "ret", "arg1", "arg2"));
        } finally {
            out.close();
        }

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        SampleUtility.main(new PrintStream(result), sample);

        String printed = new String(result.toByteArray(), "utf-8");

        assertThat(printed, is("Extracting all observations from " + sample.getAbsolutePath() + "\nCaptured call of met returned ret using [arg1, arg2]\n"));
    }
}