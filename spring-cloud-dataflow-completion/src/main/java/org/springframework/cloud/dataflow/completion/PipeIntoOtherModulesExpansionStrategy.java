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

import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;

/**
 * Continues a well-formed stream definition by adding a pipe symbol and another module,
 * provided that the stream definition hasn't reached its end yet.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
public class PipeIntoOtherModulesExpansionStrategy implements ExpansionStrategy {

	private final AppRegistry appRegistry;

	public PipeIntoOtherModulesExpansionStrategy(AppRegistry appRegistry) {
		this.appRegistry = appRegistry;
	}

	@Override
	public boolean addProposals(String text, StreamDefinition parseResult, int detailLevel,
			List<CompletionProposal> collector) {
		if (text.isEmpty() || !text.endsWith(" ")) {
			return false;
		}
		ModuleDefinition lastModule = parseResult.getDeploymentOrderIterator().next();
		// Consider "bar | foo". If there is indeed a sink named foo in the registry,
		// "foo" may also be a processor, in which case we can continue
		boolean couldBeASink = appRegistry.find(lastModule.getName(), sink) != null;
		if (couldBeASink) {
			boolean couldBeAProcessor = appRegistry.find(lastModule.getName(), processor) != null;
			if (!couldBeAProcessor) {
				return false;
			}
		}

		CompletionProposal.Factory proposals = CompletionProposal.expanding(text);
		for (AppRegistration appRegistration : appRegistry.findAll()) {
			if (appRegistration.getType() == processor || appRegistration.getType() == sink) {
				String expansion = CompletionUtils.maybeQualifyWithLabel(appRegistration.getName(), parseResult);
				collector.add(proposals.withSeparateTokens("| " + expansion,
						"Continue stream definition with a " + appRegistration.getType()));
			}
		}
		return false;
	}
}
