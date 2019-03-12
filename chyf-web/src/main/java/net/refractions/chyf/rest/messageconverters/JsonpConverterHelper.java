/*
 * Copyright 2019 Government of Canada
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.io.Writer;

public class JsonpConverterHelper extends JsonConverterHelper {
	
	public JsonpConverterHelper(Writer out) {
		super(out);
	}

	protected void writeHeader(ApiResponse response) throws IOException {
		out.write(response.getCallback() + "({");
	}
	
	protected void writeFooter(ApiResponse response) throws IOException {
		out.write("});");
	}

}
