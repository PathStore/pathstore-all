/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package pathstore.util;

/**
 * The function is defined as f: (T1, T2) -> R
 *
 * @param <T1> input type 1
 * @param <T2> input type 2
 * @param <R> return type
 */
public interface DoubleConsumerFunction<T1, T2, R> {
  /**
   * @param input1 input 1
   * @param input2 input 2
   * @return return value
   */
  R apply(final T1 input1, final T2 input2);
}
