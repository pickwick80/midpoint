/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ObjectViewDto implements Serializable {

    private String oid;
    private String name;
    private String xml;

    public ObjectViewDto() {
    }

    public ObjectViewDto(String oid, String name, String xml) {
        this.name = name;
        this.oid = oid;
        this.xml = xml;
    }

    public String getName() {
        return name;
    }

    public String getOid() {
        return oid;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }
}
