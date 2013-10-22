/*
 * Copyright 2013 Trustees of the University of Pennsylvania Licensed under the
 * 	Educational Community License, Version 2.0 (the "License"); you may
 * 	not use this file except in compliance with the License. You may
 * 	obtain a copy of the License at
 *
 * http://www.osedu.org/licenses/ECL-2.0
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an "AS IS"
 * 	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * 	or implied. See the License for the specific language governing
 * 	permissions and limitations under the License.
 */



package metridoc.ezproxy.services

import groovy.transform.ToString

/**
 * Created with IntelliJ IDEA on 10/15/13
 * @author Tommy Barker
 */
@ToString(ignoreNulls = true, includeNames = true, includePackage = false)
class CrossRefResponse {
    boolean loginFailure = false
    boolean malformedDoi = false
    boolean unresolved = false
    CrossRefResponseException statusException
    String status
    String doi
    String articleTitle
    String journalTitle
    String givenName
    String surName
    String volume
    String issue
    String firstPage
    String lastPage
    Integer printYear
    Integer electronicYear
    Integer onlineYear
    Integer nullYear
    Integer otherYear
    String printIssn
    String electronicIssn
    String printIsbn
    String electronicIsbn
}
