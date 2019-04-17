/*
 * Copyright 2018 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.gchq.palisade.example;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
//import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import uk.gov.gchq.palisade.Context;
//import uk.gov.gchq.palisade.UserId;
//import uk.gov.gchq.palisade.cache.service.impl.EtcdBackingStore;
//import uk.gov.gchq.palisade.cache.service.impl.SimpleCacheService;
//import uk.gov.gchq.palisade.client.ConfiguredClientServices;
//import uk.gov.gchq.palisade.client.ServicesFactory;
//import uk.gov.gchq.palisade.config.service.ConfigurationService;
//import uk.gov.gchq.palisade.data.service.impl.ProxyRestDataService;
//import uk.gov.gchq.palisade.example.client.ExampleConfigurator;
//import uk.gov.gchq.palisade.example.client.ExampleSimpleClient;
//import uk.gov.gchq.palisade.example.config.ExampleConfigurator;
//import uk.gov.gchq.palisade.example.data.serialiser.ExampleObjSerialiser;
//import uk.gov.gchq.palisade.mapreduce.PalisadeInputFormat;
import uk.gov.gchq.palisade.resource.LeafResource;
//import uk.gov.gchq.palisade.resource.service.impl.ProxyRestResourceService;
//import uk.gov.gchq.palisade.rest.ProxyRestConnectionDetail;
//import uk.gov.gchq.palisade.service.impl.ProxyRestPalisadeService;
//import uk.gov.gchq.palisade.service.impl.ProxyRestPolicyService;
//import uk.gov.gchq.palisade.service.request.RegisterDataRequest;
//import uk.gov.gchq.palisade.user.service.impl.ProxyRestUserService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
//import java.util.Optional;

/**
 * An example of a MapReduce job using example data from Palisade. This sets up a Palisade service which can serve
 * example data. The job is then configured to make a request as part of the MapReduce job. The actual MapReduce job is
 * a simple word count example.
 * <p>
 * The word count example is adapted from: https://hadoop.apache.org/docs/stable/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html
 */
public class AwsEmrMapReduceExample extends Configured implements Tool {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEmrMapReduceExample.class);

    protected static final String DEFAULT_OUTPUT_DIR = createOutputDir();
    private static final String RESOURCE_TYPE = "exampleObj";

    /**
     * This simple mapper example just extracts the property field of the example object and emits a count of one.
     */
    private static class ExampleMap extends Mapper<LeafResource, ExampleObj, Text, IntWritable> {
        private static final IntWritable ONE = new IntWritable(1);

        private Text outputKey = new Text();

        protected void map(final LeafResource key, final ExampleObj value, final Context context) throws IOException, InterruptedException {
            String property = value.getProperty();
            outputKey.set(property);
            context.write(outputKey, ONE);
        }
    }

    /**
     * This simple reducer example counts up the number of times we have seen each value.
     */
    private static class ExampleReduce extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();

        public void reduce(final Text key, final Iterable<IntWritable> values, final Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
            //output the totalled value
            result.set(sum);
            context.write(key, result);
        }
    }

    @Override
    public int run(final String... args) throws Exception {
        //usage check
        // ??? runPalisadeExample.sh passes 9 values ???
        if (args.length < 5) {
            System.out.println("Example file and MapReduce output directory not specified. Please provide path as argument.");
            return 1;
        }
        String sourceFile = args[0];
        String outputFolder = args[1];
        List<String> etcdEndpoints = Arrays.asList(args[2].split(","));
        String hostname = args[3];
        String dataServiceUrl = args[4];

        //create the basic job object and configure it for this example
        Job job = Job.getInstance(getConf(), "Palisade MapReduce Example");
        job.setJarByClass(AwsEmrMapReduceExample.class);

        //configure mapper
        job.setMapperClass(ExampleMap.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);

        //configure reducer
        job.setReducerClass(ExampleReduce.class);
        job.setNumReduceTasks(1);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        //set the output format
        job.setOutputFormatClass(TextOutputFormat.class);
        FileOutputFormat.setOutputPath(job, new Path(outputFolder));

        //configure the Palisade input format on an example client

        //final ConfigurationService ics = ExampleConfigurator.setupMultiJVMConfigurationService(etcdEndpoints, Optional.empty(),
        //        Optional.of(new ProxyRestPolicyService("http://" + hostname + ":8081/policy/")),
        //        Optional.of(new ProxyRestUserService("http://" + hostname + ":8083/user/")),
        //        Optional.of(new ProxyRestResourceService("http://" + hostname + ":8082/resource/")),
        //        Optional.of(new ProxyRestPalisadeService("http://" + hostname + ":8080/palisade/")),
        //        Optional.of(new SimpleCacheService().backingStore(new EtcdBackingStore().connectionDetails(etcdEndpoints, false))),
        //        Optional.of(new ProxyRestConnectionDetail().url(dataServiceUrl).serviceClass(ProxyRestDataService.class)));
        //final ConfiguredClientServices cs = new ConfiguredClientServices(ics);
        //final ExampleSimpleClient client = new ExampleSimpleClient(cs, sourceFile);

        // Edit the configuration of the Palisade requests below here
        // ==========================================================
        //configureJob(job, cs, 2);

        //next add a resource request to the job
        //addDataRequest(job, client.getURIConvertedFile(), RESOURCE_TYPE, "Alice", "Payroll");
        //addDataRequest(job, client.getURIConvertedFile(), RESOURCE_TYPE, "Bob", "Payroll");

        //launch job
        boolean success = job.waitForCompletion(true);

        return (success) ? 0 : 1;
    }

    //**
    // * Configures the given job to use this example client.
    // *
    // * @param job        the job to configure
    // * @param services   the Palisade services factory
    // * @param maxMapHint the hint for the maximum number of mappers
    // */
    //public static void configureJob(final Job job, final ServicesFactory services, final int maxMapHint) {
    //    job.setInputFormatClass(PalisadeInputFormat.class);
        //tell it which Palisade service to use
    //    PalisadeInputFormat.setPalisadeService(job, services.getPalisadeService());
        //configure the serialiser to use
    //    PalisadeInputFormat.setSerialiser(job, new ExampleObjSerialiser());
        //set the maximum mapper hint
    //    PalisadeInputFormat.setMaxMapTasksHint(job, maxMapHint);
    //}

    //
    // * Utility method to add a read request to a job.
    // *
    // * @param context       the job to add the request to
    // * @param filename      example filename
    // * @param resourceType  the example resource type
    // * @param userId        the example user id
    // * @param justification the example justification
    // */
    //public static void addDataRequest(final JobContext context, final String filename, final String resourceType, final String userId, final String justification) {
        //final RegisterDataRequest dataRequest = new RegisterDataRequest().resourceId(filename).userId(new UserId().id(userId)).context(new Context().justification(justification));
        //PalisadeInputFormat.addDataRequest(context, dataRequest);
    //}

    public static void main(final String... args) throws Exception {
        final String outputDir;
        if (args.length < 1) {
            System.out.printf("Usage: %s file [output_directory]\n", AwsEmrMapReduceExample.class.getTypeName());
            System.out.println("\nfile\tfile containing serialised ExampleObj instances to read");
            System.out.println("output_directory\tdirectory to write mapreduce outputs to");
            System.exit(1);
        }

        if (args.length < 2) {
            args[1] = DEFAULT_OUTPUT_DIR;
        }
        //remove this as it needs to be not present when the job runs
        FileUtils.deleteDirectory(new File(args[1]));
        Configuration conf = new Configuration();
        ToolRunner.run(conf, new AwsEmrMapReduceExample(), args);
    }

    private static String createOutputDir() {
        try {
            return Files.createTempDirectory("mapreduce-example-").toAbsolutePath().toString();
        } catch (IOException e) {
            LOGGER.error("Failed to create an output directory.");
            throw new RuntimeException(e);
        }
    }
}