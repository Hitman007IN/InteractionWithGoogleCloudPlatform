// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.demo.ingest.gcsbqingest.process.pipeline.csv;

import com.demo.ingest.gcsbqingest.process.schema.FieldType;
import com.demo.ingest.gcsbqingest.process.schema.SchemaUtil;
import java.util.Arrays;
import java.util.List;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

/**
 * A {@link DoFn} that detects the schema for a chunk of data.
 */
public class CsvDetectSchemaFn extends DoFn<KV<String, String[]>, KV<String, List<FieldType>>> {

	@ProcessElement
	public void detect(ProcessContext ctx) {
		KV<String, String[]> input = ctx.element();

		ctx.output(KV.of(input.getKey(), SchemaUtil.infer(Arrays.asList(input.getValue()))));
	}
}
