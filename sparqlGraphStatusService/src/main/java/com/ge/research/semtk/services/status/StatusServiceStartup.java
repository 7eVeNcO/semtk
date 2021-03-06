/**
 ** Copyright 2017 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.ge.research.semtk.services.status;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class StatusServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

  /**
   * Code to run after the service starts up.
   */
  @Override
  public void onApplicationEvent(final ApplicationReadyEvent event) {
	  
	  System.out.println("----- PROPERTIES: -----");
	  System.out.println("status.edc.services.jobEndpointType: " + event.getApplicationContext().getEnvironment().getProperty("status.edc.services.jobEndpointType"));
	  System.out.println("status.edc.services.jobEndpointDomain: " + event.getApplicationContext().getEnvironment().getProperty("status.edc.services.jobEndpointDomain"));
	  System.out.println("status.edc.services.jobEndpointServerUrl: " + event.getApplicationContext().getEnvironment().getProperty("status.edc.services.jobEndpointServerUrl"));
	  System.out.println("status.edc.services.jobEndpointDataset: " + event.getApplicationContext().getEnvironment().getProperty("status.edc.services.jobEndpointDataset"));
	  System.out.println("status.edc.services.jobEndpointUsername: " + event.getApplicationContext().getEnvironment().getProperty("status.edc.services.jobEndpointUsername"));
	  System.out.println("status.edc.services.jobEndpointPassword: " + event.getApplicationContext().getEnvironment().getProperty("status.edc.services.jobEndpointPassword"));	  
	  System.out.println("-----------------------");
	  
	  return;
  }
 
}
