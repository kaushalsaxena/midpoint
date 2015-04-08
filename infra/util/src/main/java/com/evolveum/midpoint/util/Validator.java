/**
 * Copyright (c) 2015 Evolveum
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
package com.evolveum.midpoint.util;

/**
 * Interface for object validation (mostly to be used in tests).
 * 
 * @author Radovan Semancik
 */
public interface Validator<T> {
	
	/**
	 * Validate the provided object. Throws appropriate exception if
	 * the object is not valid.
	 * 
	 * @param object object to validate
	 * @param name short string name of the object. Designed to be used in exception messages.
	 * @throws Exception appropriate exception if the object is not valid.
	 */
	void validate(T object, String name) throws Exception;

}