/*
 * jabsorb - a Java to JavaScript Advanced Object Request Broker
 * http://www.jabsorb.org
 *
 * Copyright 2007 The jabsorb team
 *
 * based on original code from
 * JSON-RPC-Java - a JSON-RPC to Java Bridge with dynamic invocation
 *
 * Copyright Metaparadigm Pte. Ltd. 2004.
 * Michael Clark <michael@metaparadigm.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.untangle.uvm.webui.jabsorb.serializer;

import org.hibernate.proxy.LazyInitializer;
import org.hibernate.proxy.pojo.cglib.CGLIBLazyInitializer;
import org.jabsorb.serializer.AbstractSerializer;
import org.jabsorb.serializer.MarshallException;
import org.jabsorb.serializer.ObjectMatch;
import org.jabsorb.serializer.SerializerState;
import org.jabsorb.serializer.UnmarshallException;

/**
 * Serialises LazyInitializer values
 */
@SuppressWarnings("serial")
public class LazyInitializerSerializer extends AbstractSerializer
{
  /**
   * Classes that this can serialise.
   */
  private static Class[] _serializableClasses = new Class[] { LazyInitializer.class, CGLIBLazyInitializer.class };

  /**
   * Classes that this can serialise to.
   */
  private static Class[] _JSONClasses = new Class[] { String.class };

  public Class[] getJSONClasses()
  {
    return _JSONClasses;
  }

  public Class[] getSerializableClasses()
  {
    return _serializableClasses;
  }

  public Object marshall(SerializerState state, Object p, Object o)
      throws MarshallException
  {
      return null;
  }

  public ObjectMatch tryUnmarshall(SerializerState state, Class clazz,
      Object jso) throws UnmarshallException
  {
    state.setSerialized(jso, ObjectMatch.NULL);
    return ObjectMatch.NULL;
  }

  public Object unmarshall(SerializerState state, Class clazz, Object jso)
      throws UnmarshallException
  {
    return null;
  }

}
