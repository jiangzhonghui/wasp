package com.github.nr4bt.wasp;

import com.github.nr4bt.wasp.http.Body;
import com.github.nr4bt.wasp.http.Header;
import com.github.nr4bt.wasp.http.Path;
import com.github.nr4bt.wasp.http.Query;
import com.github.nr4bt.wasp.http.RestMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Orhan Obut
 */
final class MethodInfo {

    private final Method method;

    private String relativeUrl;
    private String httpMethod;
    private Type responseObjectType;
    private Annotation[] methodAnnotations;

    private MethodInfo(Method method) {
        this.method = method;

        init();
    }

    synchronized void init() {
        parseMethodAnnotations();
        parseResponseObjectType();
        parseParamAnnotations();
    }

    static MethodInfo newInstance(Method method) {
        return new MethodInfo(method);
    }

    private void parseMethodAnnotations() {
        Annotation[] annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            RestMethod methodInfo = null;

            // Look for a @RestMethod annotation on the parameter annotation indicating request method.
            for (Annotation innerAnnotation : annotationType.getAnnotations()) {
                if (RestMethod.class == innerAnnotation.annotationType()) {
                    methodInfo = (RestMethod) innerAnnotation;
                    break;
                }
            }
            if (methodInfo == null) {
                throw new NullPointerException("method annotation may not be null");
            }
            String path;
            try {
                path = (String) annotationType.getMethod("value").invoke(annotation);
            } catch (Exception e) {
                throw methodError("Failed to extract String 'value' from @%s annotation.",
                        annotationType.getSimpleName());
            }
            relativeUrl = path;
            httpMethod = methodInfo.value();
        }
    }

    private void parseResponseObjectType() {
        Type[] parameterTypes = method.getGenericParameterTypes();
        if (parameterTypes.length == 0) {
            throw new IllegalArgumentException("Callback should be added as param");
        }
        Type lastArgType;
        Class<?> lastArgClass = null;

        Type typeToCheck = parameterTypes[parameterTypes.length - 1];
        lastArgType = typeToCheck;
        if (typeToCheck instanceof ParameterizedType) {
            typeToCheck = ((ParameterizedType) typeToCheck).getRawType();
        }
        if (typeToCheck instanceof Class) {
            lastArgClass = (Class<?>) typeToCheck;
        }
        if (!CallBack.class.isAssignableFrom(lastArgClass)) {
            throw new IllegalArgumentException("Last param should be CallBack");
        }

        lastArgType = RetroTypes.getSupertype(lastArgType, RetroTypes.getRawType(lastArgType), CallBack.class);
        if (lastArgType instanceof ParameterizedType) {
            responseObjectType = getParameterUpperBound((ParameterizedType) lastArgType);
        }
    }

    private void parseParamAnnotations() {
        Annotation[][] annotationArrays = method.getParameterAnnotations();
        methodAnnotations = new Annotation[annotationArrays.length];

        List<String> pathParams = new ArrayList<>();
        List<String> queryParams = new ArrayList<>();
        List<String> headerParams = new ArrayList<>();

        int count = annotationArrays.length;
        for (int i = 0; i < count; i++) {
            Annotation annotationResult = null;
            for (Annotation annotation : annotationArrays[i]) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                if (annotationType == Path.class) {
                    //TODO validate
                    String value = ((Path) annotation).value();
                    if (pathParams.contains(value)) {
                        throw new IllegalArgumentException("Path name should not be duplicated");
                    }
                    pathParams.add(value);
                }
                if (annotationType == Body.class) {
                    //TODO validate
                }
                if (annotationType == Query.class) {
                    //TODO validate
                    String value = ((Query) annotation).value();
                    if (queryParams.contains(value)) {
                        throw new IllegalArgumentException("Query name should not be duplicated");
                    }
                    queryParams.add(value);
                }
                if (annotationType == Header.class) {
                    String value = ((Header) annotation).value();
                    if (headerParams.contains(value)) {
                        throw new IllegalArgumentException("Header name should not be duplicated");
                    }
                    headerParams.add(value);
                }

                annotationResult = annotation;
            }
            methodAnnotations[i] = annotationResult;
        }
    }

    private static Type getParameterUpperBound(ParameterizedType type) {
        Type[] types = type.getActualTypeArguments();
        for (int i = 0; i < types.length; i++) {
            Type paramType = types[i];
            if (paramType instanceof WildcardType) {
                types[i] = ((WildcardType) paramType).getUpperBounds()[0];
            }
        }
        return types[0];
    }

    private RuntimeException methodError(String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        return new IllegalArgumentException(
                method.getDeclaringClass().getSimpleName() + "." + method.getName() + ": " + message);
    }

    String getRelativeUrl() {
        return relativeUrl;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    Type getResponseObjectType() {
        return responseObjectType;
    }

    Annotation[] getMethodAnnotations() {
        return methodAnnotations;
    }
}
