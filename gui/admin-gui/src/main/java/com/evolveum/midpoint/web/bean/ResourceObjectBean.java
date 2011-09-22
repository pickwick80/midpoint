/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.web.bean;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author lazyman
 * 
 */
public class ResourceObjectBean extends ObjectBean {

	private static final long serialVersionUID = 3229226312145337162L;
	private Map<String, String> attributes;

	public ResourceObjectBean(String oid, String name, Map<String, String> attributes) {
		super(oid, name);
		this.attributes = attributes;
	}

	public Map<String, String> getAttributes() {
		if (attributes == null) {
			attributes = new HashMap<String, String>();
		}
		return attributes;
	}
}
