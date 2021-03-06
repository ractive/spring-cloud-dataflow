/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.completion;

import static org.springframework.cloud.dataflow.core.ArtifactType.processor;
import static org.springframework.cloud.dataflow.core.ArtifactType.sink;

import java.util.List;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.dsl.CheckPointedParseException;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;

/**
 * Provides completions for the case where the user has entered a pipe
 * symbol and a module reference is expected next.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
public class ModulesAfterPipeRecoveryStrategy extends
		StacktraceFingerprintingRecoveryStrategy<CheckPointedParseException> {

	private final AppRegistry appRegistry;

	ModulesAfterPipeRecoveryStrategy(AppRegistry appRegistry) {
		super(CheckPointedParseException.class, "foo |", "foo | ");
		this.appRegistry = appRegistry;
	}


	@Override
	public void addProposals(String dsl, CheckPointedParseException exception,
			int detailLevel, List<CompletionProposal> collector) {

		StreamDefinition streamDefinition = new StreamDefinition("__dummy",
				exception.getExpressionStringUntilCheckpoint());

		CompletionProposal.Factory proposals = CompletionProposal.expanding(dsl);

		// We only support full streams at the moment, so completions can only be processor or sink
		for (AppRegistration appRegistration : appRegistry.findAll()) {
			if (appRegistration.getType() == processor || appRegistration.getType() == sink) {
				String expansion = CompletionUtils.maybeQualifyWithLabel(appRegistration.getName(), streamDefinition);
				collector.add(proposals.withSeparateTokens(expansion,
						"Continue stream definition with a " + appRegistration.getType()));
			}
		}
	}
}
