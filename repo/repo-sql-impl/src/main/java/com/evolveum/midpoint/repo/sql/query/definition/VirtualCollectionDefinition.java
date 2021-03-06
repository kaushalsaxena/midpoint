/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.repo.sql.query.definition;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class VirtualCollectionDefinition extends CollectionDefinition {

    private VirtualQueryParam[] additionalParams;

    public VirtualCollectionDefinition(QName jaxbName, Class jaxbType, String propertyName, Class propertyType) {
        super(jaxbName, jaxbType, propertyName, propertyType);
    }

    public VirtualQueryParam[] getAdditionalParams() {
        return additionalParams;
    }

    void setAdditionalParams(VirtualQueryParam[] additionalParams) {
        this.additionalParams = additionalParams;
    }

    @Override
    protected void toStringExtended(StringBuilder builder) {
        super.toStringExtended(builder);

        builder.append(", params=").append(additionalParams.length);
    }

    @Override
    protected String getDebugDumpClassName() {
        return "VirtualCol";
    }
}
