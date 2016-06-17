package com.keyroy.util.json;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.m.JSONArray;
import org.json.m.JSONObject;
import org.json.m.JSONTokener;

public class Json {
	private static final String CLASS_KEY = "class";
	// json ��Դ
	private JSONObject source;

	public Json() {
		source = new JSONObject();
	}

	public Json(String json) {
		if (json.startsWith("[")) {
			source = new JSONArray(json);
		} else {
			source = new JSONObject(json);
		}
	}

	public Json(JSONObject source) {
		this.source = source;
	}

	public Json(InputStream inputStream) {
		try {
			this.source = new JSONObject(new JSONTokener(inputStream));
			inputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Json(InputStream inputStream, Charset charset) {
		try {
			this.source = new JSONObject(new JSONTokener(inputStream, charset));
			inputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Json(Object object) {
		try {
			this.source = encode(object, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public final <T> List<T> toList(Class<T> clazz) throws Exception {
		return toList(clazz, new ArrayList<T>());
	}

	@SuppressWarnings("unchecked")
	public final <T> List<T> toList(Class<T> clazz, List<T> list) throws Exception {
		if (source != null && source instanceof JSONArray) {
			JSONArray jsonArray = (JSONArray) source;
			for (int i = 0; i < jsonArray.length(); i++) {
				if (ReflectTools.isBaseType(clazz)) {
					Object object = jsonArray.get(i);
					list.add((T) object);
				} else {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					T t = decode(clazz, jsonObject);
					list.add(t);
				}
			}
			return list;
		}
		return null;
	}

	public final <T> T toObject(Class<T> clazz) throws Exception {
		if (source != null) {
			return decode(clazz, source);
		} else {
			return null;
		}
	}

	public final JSONObject getSource() {
		return source;
	}

	@Override
	public String toString() {
		if (source != null) {
			return source.toString();
		}
		return super.toString();
	}

	// ------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	/***
	 * ��ʼ������ JSONObject
	 * 
	 * @param source
	 *            Ŀ�����
	 * @param template
	 * @return
	 * @throws Exception
	 */
	public static final JSONObject encode(Object source, Class<?> template) throws Exception {
		if (source != null) {
			if (source instanceof JSONObject) {
				return (JSONObject) source;
			} else {
				JSONObject jsonObject = null;
				Class<?> clazz = ReflectTools.getClass(source);
				if (ReflectTools.isBaseType(clazz)) {// ����
					jsonObject = new JSONObject();
					jsonObject.append(clazz.getSimpleName(), String.valueOf(source));
				} else if (ReflectTools.isArray(source)) { // ����
					Class<?> arrayClass = clazz.getComponentType();
					JSONArray jsonArray = new JSONArray();
					Object[] objects = (Object[]) source;
					for (Object object : objects) {
						if (object != null) {
							if (ReflectTools.isBaseType(arrayClass)) { // ��������
								jsonArray.put(object);
							} else { // ��������
								JSONObject childJsonObject = encode(object, null);
								jsonArray.put(childJsonObject);
							}
						}
					}
					//
					jsonObject = jsonArray;
				} else if (List.class.isInstance(source)) { // �б�
					JSONArray jsonArray = new JSONArray();
					List list = (List) source;
					for (Object object : list) {
						if (object != null) {
							if (ReflectTools.isBaseType(object.getClass())) { // ��������
								jsonArray.put(object);
							} else { // ��������
								JSONObject childJsonObject = encode(object, template);
								if (template != null && template.equals(object.getClass()) == false) {
									childJsonObject.append(CLASS_KEY, object.getClass().getName());
								}
								jsonArray.put(childJsonObject);
							}
						}
					}
					//
					jsonObject = jsonArray;
				} else if (Map.class.isInstance(source)) { // ��Ӧ��
					jsonObject = new JSONObject();
					Map<?, ?> map = (Map<?, ?>) source;
					Set<?> set = map.keySet();
					Iterator<?> iterator = set.iterator();
					while (iterator.hasNext()) {
						Object key = (Object) iterator.next();
						Object value = map.get(key);
						if (ReflectTools.isBaseType(value.getClass())) { // ��������
							jsonObject.append(String.valueOf(key), value);
						} else { // ��������
							JSONObject childJsonObject = encode(value, null);
							jsonObject.append(String.valueOf(key), childJsonObject);
						}
					}
				} else if (source instanceof Class<?>) {
				} else {
					jsonObject = new JSONObject();
					List<Field> fields = ReflectTools.getFields(clazz);
					for (Field field : fields) {
						Object value = field.get(source);
						String fieldName = getFieldName(field);
						if (value != null) {
							if (ReflectTools.isBaseType(value.getClass())) { // ��������
								JsonAn jsonAn = field.getAnnotation(JsonAn.class);
								if (ReflectTools.isDefaultValue(value)
										&& (jsonAn == null || jsonAn.showDefault() == false)) {
									// Ĭ��ֵ
								} else {
									jsonObject.append(fieldName, value);
								}
							} else if (ReflectTools.isSubClass(field, List.class)) {
								JSONObject childJsonObject = encode(value,
										ReflectTools.getClass(field.getGenericType()));
								jsonObject.append(fieldName, childJsonObject);
							} else if (field.getType().equals(clazz) == false) { // ��������
								JSONObject childJsonObject = encode(value, null);
								// �鿴���� �� ���������Ƿ���ͬ
								Class<?> valueClass = value.getClass();
								if (valueClass.equals(template)) { // �������ͺ�ģ��������ͬ

								} else if (value.getClass().equals(field.getType())) {// �������ͺ�����������ͬ

								} else {
									childJsonObject.append(CLASS_KEY, value.getClass().getName());
								}
								jsonObject.append(fieldName, childJsonObject);
							}
						}
					}
				}
				return jsonObject;
			}
		}
		return null;
	}

	public static final void fill(Object t, InputStream inputStream) throws Exception {
		fill(t, new JSONObject(new JSONTokener(inputStream)));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static final void fill(Object t, JSONObject source) throws Exception {
		if (t instanceof Class<?>) {
			throw new IllegalArgumentException("object can not been Class");
		} else {
			List<Field> fields = ReflectTools.getFields(t.getClass());
			for (Field field : fields) {
				String fieldName = getFieldName(field);
				if (source.has(fieldName)) {
					Object value = source.get(fieldName);
					if (ReflectTools.isBaseType(field.getType()) && ReflectTools.isBaseType(value.getClass())) { // ��������
						if (value.getClass().equals(field.getType())) { // ��������ƥ��
							field.set(t, value);
						} else {
							field.set(t, ReflectTools.parser(String.valueOf(value), field.getType()));
						}
					} else if (ReflectTools.isArray(field) && value instanceof JSONArray) { // ����
						Class<?> arrayClass = ReflectTools.getTemplate(field);
						JSONArray jsonArray = (JSONArray) value;
						Object array = Array.newInstance(arrayClass, jsonArray.length());
						field.set(t, array);
						if (ReflectTools.isBaseType(arrayClass)) { // ������������
							for (int i = 0; i < jsonArray.length(); i++) {
								Array.set(array, i, jsonArray.get(i));
							}
						} else { // ������������
							for (int i = 0; i < jsonArray.length(); i++) {
								Object jsonElement = jsonArray.get(i);
								if (jsonElement instanceof JSONObject) {
									Object element = decode(arrayClass, (JSONObject) jsonElement);
									Array.set(array, i, element);
								}
							}
						}

					} else if (ReflectTools.isSubClass(field, List.class) && value instanceof JSONArray) { // �б�
						JSONArray jsonArray = (JSONArray) value;
						Class<?> arrayClass = ReflectTools.getTemplate(field);
						List list = null;
						if (field.getType().equals(List.class)) {
							list = new ArrayList<Object>(jsonArray.length());
						} else {
							list = (List) field.getType().newInstance();
						}
						field.set(t, list);
						if (ReflectTools.isBaseType(arrayClass)) { // ������������
							for (int i = 0; i < jsonArray.length(); i++) {
								list.add(jsonArray.get(i));
							}
						} else { // ������������
							for (int i = 0; i < jsonArray.length(); i++) {
								Object jsonElement = jsonArray.get(i);
								if (jsonElement instanceof JSONObject) {
									JSONObject jsonObject = (JSONObject) jsonElement;
									if (jsonObject.has(CLASS_KEY)) {
										Class<?> elementClass = Class.forName(jsonObject.getString(CLASS_KEY));
										list.add(decode(elementClass, jsonObject));
									} else {
										list.add(decode(arrayClass, jsonObject));
									}
								}
							}
						}
					} else if (ReflectTools.isSubClass(field, Map.class) && value instanceof JSONObject) {// ��Ӧ��
						JSONObject jsonObject = (JSONObject) value;
						Class<?>[] classes = ReflectTools.getTemplates(field);

						Class<?> keyClass = classes[0];
						Class<?> valClass = classes[1];

						Map map = null;
						try {
							String classPath = (String) jsonObject.remove(CLASS_KEY);
							map = (Map) Class.forName(classPath).newInstance();
						} catch (Exception e) {
						}

						if (map == null) {
							if (field.getType().equals(Map.class)) {
								map = new HashMap(jsonObject.length());
							} else {
								map = (Map) field.getType().newInstance();
							}
						}

						field.set(t, map);
						String[] keies = JSONObject.getNames(jsonObject);
						for (String key : keies) {
							Object mapKey = key;
							Object mapValue = jsonObject.get(key);
							if (keyClass != null) {
								mapKey = ReflectTools.parser(key, keyClass);
							}
							if (mapValue != null && ReflectTools.isBaseType(mapValue.getClass())) {
								map.put(mapKey, mapValue);
							} else if (mapValue instanceof JSONObject && valClass != null) {
								Object object = decode(valClass, (JSONObject) mapValue);
								map.put(mapKey, object);
							}
						}
					} else if (value instanceof JSONObject) { // ��������
						JSONObject jsonObject = (JSONObject) value;
						Object object = null;
						if (jsonObject.has(CLASS_KEY)) {
							Class<?> elementClass = Class.forName(jsonObject.getString(CLASS_KEY));
							object = decode(elementClass, jsonObject);
						} else {
							object = decode(field.getType(), jsonObject);
						}
						field.set(t, object);
					}
				}
			}
		}

	}

	public static final <T> T decode(Class<T> clazz, JSONObject source) throws Exception {
		T t = clazz.newInstance();
		fill(t, source);
		return t;
	}

	private static final String getFieldName(Field field) {
		JsonAn jsonAn = field.getAnnotation(JsonAn.class);
		if (jsonAn != null && jsonAn.value().length() > 0) {
			return jsonAn.value();
		}
		return field.getName();
	}

}
