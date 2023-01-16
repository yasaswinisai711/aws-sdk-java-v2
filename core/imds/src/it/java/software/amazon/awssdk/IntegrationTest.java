/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.imds.Ec2MetadataAsyncClient;
import software.amazon.awssdk.imds.Ec2MetadataClient;
import software.amazon.awssdk.imds.Ec2MetadataResponse;

class IntegrationTest {

    @Test
    void integrationTest() {
        Ec2MetadataClient client = Ec2MetadataClient.create();
        Ec2MetadataResponse response = client.get("/latest/meta-data/");
        System.out.println(response);
    }

    @Test
    void asyncIntegrationTest() {
        Ec2MetadataAsyncClient client = Ec2MetadataAsyncClient.create();
        Ec2MetadataResponse response = client.get("/latest/meta-data/").join();
        System.out.println(response);
    }

}
