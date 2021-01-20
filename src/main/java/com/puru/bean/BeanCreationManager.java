package com.puru.bean;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cglib.beans.BeanGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class BeanCreationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanCreationManager.class);

    private static String BASE_MAPPER_PACKAGE_SCAN_PATH = "com.puru.bean";

    private static String SETTER_METHOD_PREFIX = "set";

    private String mappingExcelsFolder;

    private MapperRegistry mapperRegistry;

    private MapperFunctionRegistry mapperFunctionRegistry;

    private ApplicationContext applicationContext;

    public void init() throws Exception {

        // All mapper functions should be registered before registering mappers.
        registerMapperFunctions();
        registerMappers();
    }

    /**
     * Register custom function using the com.puru.bean.Mapper annotation.
     * <p>
     * Mapper annotation accepts two attributes A. setterClass (One or Multiple) - Specify the .class of the type that
     * needs to be injected into the mapper. Make sure that the mapper class contains respective setter method and the
     * respective bean is available in the spring context. The name of the setter method should match the object name as
     * example provided below. Example : CustomMapperClass contains Annotation : @Mapper(setterClass = {
     * DateMapper.class }) SetterMethod: setCutOffDate(DateMapper dateMapper)
     * <p>
     * @throws Exception
     */
    private void registerMapperFunctions() throws Exception {

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(Mapper.class));

        // Get all the bean definitions of classes that have been annotated with the @Mapper annotation
        Set<BeanDefinition> definitions = scanner.findCandidateComponents(BASE_MAPPER_PACKAGE_SCAN_PATH);

        for (BeanDefinition definition : definitions) {

            String setterMethodName = null;

            String fieldName = null;

            try {

                // Instantiate object using the bean definition retrieved by the scanner.
                Class classObject = BeanGenerator.class.getClassLoader().loadClass(definition.getBeanClassName());

                Object classObjectInstance = classObject.getConstructor().newInstance();

                // Get all the defined attributes from the annotated mapper.
                Mapper mapperInfo = (Mapper) classObject.getAnnotation(Mapper.class);

                Class[] setterClassArr = mapperInfo.setterClass();

                String[] setterAttibuteArr = mapperInfo.setterAttribute();

                // Skip if Mapper object has no objects to inject
                if (setterClassArr.length != 0) {

                    for (Class setterClass : setterClassArr) {

                        // Get the bean that need to be injected in to the mapper from the spring context.
                        Object setterObject = applicationContext.getBean(setterClass);

                        setterMethodName = SETTER_METHOD_PREFIX.concat(setterClass.getSimpleName());

                        Method setterMethod = classObjectInstance.getClass().getMethod(setterMethodName, setterClass);

                        setterMethod.invoke(classObjectInstance, setterObject);
                    }
                }

                // Skip if there are no attributes that have to be set.
                if (setterAttibuteArr.length != 0) {

                    for (String setterAttibute : setterAttibuteArr) {

                        fieldName = setterAttibute;

                        // Look for the attribute in the current class which is injected by the respective configuration
                        Field setterAttributeField = this.getClass().getDeclaredField(fieldName);

                        // Because its a private method set its accessibility to true.
                        setterAttributeField.setAccessible(true);

                        Object returnValue = setterAttributeField.get(this);

                        setterMethodName = SETTER_METHOD_PREFIX + StringUtils.capitalize(fieldName);

                        Method setterMethod = classObjectInstance.getClass()
                                .getMethod(setterMethodName, returnValue.getClass());

                        setterMethod.invoke(classObjectInstance, returnValue);
                    }

                }

                MapperFunction mapperFunctionInstance = (MapperFunction) classObjectInstance;

                mapperFunctionRegistry.register(mapperFunctionInstance);

            } catch (ClassNotFoundException classNotFoundException) {
                LOGGER.error("Unable to register Mapper Function : [{}] because class does not exist",
                        definition.getBeanClassName());
                throw new InstantiationException("Unable to instantiate Mapper Function : ["
                        + definition.getBeanClassName() + "] because class does not exist.");

            } catch (NoSuchFieldException noSuchFieldException) {
                LOGGER.error("Unable to register Mapper Function : [{}] because Field:[{}] does not exist",
                        definition.getBeanClassName(), fieldName);
                throw new InstantiationException("Unable to instantiate Mapper Function : ["
                        + definition.getBeanClassName() + "] because Filed: " + fieldName + " does not exist.");

            } catch (NoSuchMethodException noSuchMethodException) {
                if (setterMethodName == null)
                    setterMethodName = "Constructor";
                LOGGER.error("Unable to register Mapper Function : [{}] because methodName:[{}] does not exist",
                        definition.getBeanClassName(), setterMethodName);
                throw new InstantiationException("Unable to instantiate Mapper Function : ["
                        + definition.getBeanClassName() + "] because Method: " + setterMethodName + " does not exist.");

            } catch (Exception exception) {
                LOGGER.error("Unable to register Mapper Function : [{}]", definition.getBeanClassName());
                throw new InstantiationException(
                        "Unable to instantiate Mapper Function : [" + definition.getBeanClassName() + "]");
            }

        }
    }

    private void registerMappers() throws Exception {

        if (StringUtils.isEmpty(mappingExcelsFolder)) {
            LOGGER.warn("mapper.folder is not set, hence skipping mappers.");
            return;
        }
        /*
         * if (!(new File(mappingExcelsFolder).exists())) { LOGGER.warn("mapper.folder " + mappingExcelsFolder +
         * " does not exist, hence skipping mappers."); return; }
         */
        final List<InputStream> mappingFiles = new LocalFileManger().getInputStreams(mappingExcelsFolder, "xlsx", true);
        mappingFiles.stream().forEach(inputStream -> mapperRegistry.registerMapperFromInputStream(inputStream));
    }
}
