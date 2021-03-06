package io.cucumber.cucumberexpressions;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class ParameterTypeRegistry {
    private static final List<String> INTEGER_REGEXPS = asList("-?\\d+", "\\d+");
    private static final List<String> FLOAT_REGEXPS = singletonList("-?\\d*[\\.,]\\d+");
    private static final List<String> HEX_REGEXPS = singletonList("0[xX][0-9a-fA-F]{2}");
    private static final List<String> WORD_REGEXPS = singletonList("\\w+");
    private static final List<String> STRING_REGEXPS = singletonList("\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)\"|\\'([^\\'\\\\]*(\\\\.[^\\'\\\\]*)*)\\'");

    private final Map<String, ParameterType<?>> parameterTypeByName = new HashMap<>();
    private final Map<String, SortedSet<ParameterType<?>>> parameterTypesByRegexp = new HashMap<>();

    public ParameterTypeRegistry(Locale locale) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        NumberParser numberParser = new NumberParser(numberFormat);

        defineParameterType(new ParameterType<>("bigint", INTEGER_REGEXPS, BigInteger.class, new SingleTransformer<BigInteger>(BigInteger::new), false, false));
        defineParameterType(new ParameterType<>("bigdecimal", INTEGER_REGEXPS, BigDecimal.class, new SingleTransformer<BigDecimal>(BigDecimal::new), false, false));
        defineParameterType(new ParameterType<>("byte", HEX_REGEXPS, Byte.class, new SingleTransformer<Byte>(Byte::decode), false, false));
        defineParameterType(new ParameterType<>("short", INTEGER_REGEXPS, Short.class, new SingleTransformer<Short>(Short::decode), false, false));
        defineParameterType(new ParameterType<>("int", INTEGER_REGEXPS, Integer.class, new SingleTransformer<Integer>(Integer::decode), true, true));
        defineParameterType(new ParameterType<>("long", INTEGER_REGEXPS, Long.class, new SingleTransformer<Long>(Long::decode), false, false));
        defineParameterType(new ParameterType<>("float", FLOAT_REGEXPS, Float.class, new SingleTransformer<Float>(numberParser::parseFloat), false, false));
        defineParameterType(new ParameterType<>("double", FLOAT_REGEXPS, Double.class, new SingleTransformer<Double>(numberParser::parseDouble), true, true));
        defineParameterType(new ParameterType<>("word", WORD_REGEXPS, String.class, new SingleTransformer<String>(s -> s), false, false));
        defineParameterType(new ParameterType<>("string", STRING_REGEXPS, String.class, new SingleTransformer<String>(s -> s.replaceAll("\\\\\"", "\"").replaceAll("\\\\'", "'")), true, false));
    }

    public void defineParameterType(ParameterType<?> parameterType) {
        if (parameterTypeByName.containsKey(parameterType.getName()))
            throw new DuplicateTypeNameException(String.format("There is already a parameter type with name %s", parameterType.getName()));
        parameterTypeByName.put(parameterType.getName(), parameterType);

        for (String parameterTypeRegexp : parameterType.getRegexps()) {
            SortedSet<ParameterType<?>> parameterTypes = parameterTypesByRegexp
                    .computeIfAbsent(parameterTypeRegexp, r -> new TreeSet<>());
            if (!parameterTypes.isEmpty() && parameterTypes.first().preferForRegexpMatch() && parameterType.preferForRegexpMatch()) {
                throw new CucumberExpressionException(String.format(
                        "There can only be one preferential parameter type per regexp. " +
                                "The regexp /%s/ is used for two preferential parameter types, {%s} and {%s}",
                        parameterTypeRegexp, parameterTypes.first().getName(), parameterType.getName()
                ));
            }
            parameterTypes.add(parameterType);
        }
    }

    public <T> ParameterType<T> lookupByTypeName(String typeName) {
        return (ParameterType<T>) parameterTypeByName.get(typeName);
    }

    public <T> ParameterType<T> lookupByRegexp(String parameterTypeRegexp, Pattern expressionRegexp, String text) {
        SortedSet<ParameterType<?>> parameterTypes = parameterTypesByRegexp.get(parameterTypeRegexp);
        if (parameterTypes == null) return null;
        if (parameterTypes.size() > 1 && !parameterTypes.first().preferForRegexpMatch()) {
            // We don't do this check on insertion because we only want to restrict
            // ambiguity when we look up by Regexp. Users of CucumberExpression should
            // not be restricted.
            List<GeneratedExpression> generatedExpressions = new CucumberExpressionGenerator(this).generateExpressions(text);
            throw new AmbiguousParameterTypeException(parameterTypeRegexp, expressionRegexp, parameterTypes, generatedExpressions);
        }
        return (ParameterType<T>) parameterTypes.first();
    }

    public Collection<ParameterType<?>> getParameterTypes() {
        return parameterTypeByName.values();
    }
}
