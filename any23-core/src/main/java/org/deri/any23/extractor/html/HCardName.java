/*
 * Copyright 2008-2010 Digital Enterprise Research Institute (DERI)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deri.any23.extractor.html;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An HCard name, consisting of various parts. Handles computation
 * of full names from first and last names, and similar computations.
 *
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class HCardName {

    public static final String GIVEN_NAME = "given-name";
    public static final String FAMILY_NAME = "family-name";
    public static final String ADDITIONAL_NAME = "additional-name";
    public static final String NICKNAME = "nickname";
    public static final String HONORIFIC_PREFIX = "honorific-prefix";
    public static final String HONORIFIC_SUFFIX = "honorific-suffix";

    public static final String[] FIELDS = {
            GIVEN_NAME,
            FAMILY_NAME,
            ADDITIONAL_NAME,
            NICKNAME,
            HONORIFIC_PREFIX,
            HONORIFIC_SUFFIX
    };

    private static final String[] NAME_COMPONENTS = {
            HONORIFIC_PREFIX,
            GIVEN_NAME,
            ADDITIONAL_NAME,
            FAMILY_NAME,
            HONORIFIC_SUFFIX
    };

    private Map<String, FieldValue> fields = new HashMap<String, FieldValue>();
    private String[] fullName   = null;
    private String organization = null;
    private String unit         = null;

    public void setField(String fieldName, String value) {
        value = fixWhiteSpace(value);
        if (value == null) return;
        FieldValue fieldValue = fields.get(fieldName);
        if(fieldValue == null) {
            fieldValue = new FieldValue();
            fields.put(fieldName, fieldValue);
        }
        fieldValue.addValue(value);
    }

    public void setFullName(String value) {
        value = fixWhiteSpace(value);
        if (value == null) return;
        String[] split = value.split("\\s+");
        // Supporting case: ['King,',  'Ryan'] that is converted to ['Ryan', 'King'] .
        final String split0 = split[0];
        final int split0Length = split0.length();
        if(split.length > 1 && split0.charAt(split0Length -1) == ',') {
            String swap = split[1];
            split[1] = split0.substring(0, split0Length -1);
            split[0] = swap;
        }
        this.fullName = split;
    }

    public void setOrganization(String value) {
        value = fixWhiteSpace(value);
        if (value == null) return;
        this.organization = value;
    }

    public boolean isMultiField(String fieldName) {
        FieldValue fieldValue = fields.get(fieldName);
        return fieldValue != null && fieldValue.isMultiField();
    }

    public boolean containsField(String fieldName) {
        return GIVEN_NAME.equals(fieldName) || FAMILY_NAME.equals(fieldName) || fields.containsKey(fieldName);
    }

    public String getField(String fieldName) {
        if (GIVEN_NAME.equals(fieldName)) {
            return getFullNamePart(GIVEN_NAME, 0);
        }
        if (FAMILY_NAME.equals(fieldName)) {
            return getFullNamePart(FAMILY_NAME, Integer.MAX_VALUE);
        }
        FieldValue v = fields.get(fieldName);
        return v == null ? null : v.getValue();
    }

    public Collection<String> getFields(String fieldName) {
        FieldValue v = fields.get(fieldName);
        return v == null ? Collections.<String>emptyList() : v.getValues();
    }

    private String getFullNamePart(String fieldName, int index) {
        if (fields.containsKey(fieldName)) {
            return fields.get(fieldName).getValue();
        }
        if (fullName == null) return null;
        // If org and fn are the same, the hCard is for an organization, and we do not split the fn
        if (fullName[0].equals(organization)) {
            return null;
        }
        if (index != Integer.MAX_VALUE && fullName.length <= index) return null;
        return fullName[ index == Integer.MAX_VALUE ? fullName.length - 1 : index];
    }

    public boolean hasField(String fieldName) {
        return getField(fieldName) != null;
    }

    public boolean hasAnyField() {
        for (String fieldName : FIELDS) {
            if (hasField(fieldName)) return true;
        }
        return false;
    }

    public String getFullName() {
        if (fullName != null) return join(fullName, " ");
        StringBuffer s = new StringBuffer();
        boolean empty = true;
        for (String fieldName : NAME_COMPONENTS) {
            if (!hasField(fieldName)) continue;
            if (!empty) {
                s.append(' ');
            }
            s.append(getField(fieldName));
            empty = false;
        }
        if (empty) return null;
        return s.toString();
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganizationUnit(String value) {
        value = fixWhiteSpace(value);
        if (value == null) return;
        this.unit = value;
    }

    public String getOrganizationUnit() {
        return unit;
    }

    private static String join(String[] sarray, String delimiter) {
        StringBuilder builder = new StringBuilder();
        final int sarrayLengthMin2 =  sarray.length - 1;
        for(int i = 0; i < sarray.length; i++) {
            builder.append(sarray[i]);
            if( i < sarrayLengthMin2) {
                builder.append(delimiter);
            }
        }
        return builder.toString();
    }

    private String fixWhiteSpace(String s) {
        if (s == null) return null;
        s = s.trim().replaceAll("\\s+", " ");
        if ("".equals(s)) return null;
        return s;
    }



    /**
     * Represents a possible field value.
     */
    private class FieldValue {

        private String value;

        private List<String> multiValue = new ArrayList<String>();

        FieldValue() {}

        void addValue(String v) {
            if(value == null && multiValue == null) {
                value = v;
            } else if(multiValue == null) {
                multiValue = new ArrayList<String>();
                multiValue.add(value);
                value = null;
                multiValue.add(v);
            } else {
                multiValue.add(v);
            }
        }

        boolean isMultiField() {
            return value == null;
        }

        String getValue() {
            return value != null ? value : multiValue.get(0);
        }

        Collection<String> getValues() {
            return value != null ? Arrays.asList(value) : multiValue;
        }
        
    }
    
}