(ns hyperion.riak.map-reduce.json-parse)

; Modified JSON parser that returns numbers as strings
; This fixes an issue where numbers larger than the
; Javascript MAX_NUMBER will loose presicion.
(def json-parse-js
  "
var json_parse = (function () {

    var at,     // The index of the current character
        ch,     // The current character
        escapee = {
            '\"':  '\"',
            '\\\\': '\\\\',
            '/':  '/',
            b:    '\\b',
            f:    '\\f',
            n:    '\\n',
            r:    '\\r',
            t:    '\\t'
        },
        text,

        error = function (m) {

            throw {
                name:    'SyntaxError',
                message: m,
                at:      at,
                text:    text
            };
        },

        next = function (c) {

            if (c && c !== ch) {
                error(\"Expected '\" + c + \"' instead of '\" + ch + \"'\");
            }

            ch = text.charAt(at);
            at += 1;
            return ch;
        },

        number = function () {

            var number,
                string = '';

            if (ch === '-') {
                string = '-';
                next('-');
            }
            while (ch >= '0' && ch <= '9') {
                string += ch;
                next();
            }
            if (ch === '.') {
                string += '.';
                while (next() && ch >= '0' && ch <= '9') {
                    string += ch;
                }
            }
            if (ch === 'e' || ch === 'E') {
                string += ch;
                next();
                if (ch === '-' || ch === '+') {
                    string += ch;
                    next();
                }
                while (ch >= '0' && ch <= '9') {
                    string += ch;
                    next();
                }
            }
            number = +string;
            if (!isFinite(number)) {
                error(\"Bad number\");
            } else {
                return string;
            }
        },

        string = function () {


            var hex,
                i,
                string = '',
                uffff;


            if (ch === '\"') {
                while (next()) {
                    if (ch === '\"') {
                        next();
                        return string;
                    }
                    if (ch === '\\\\') {
                        next();
                        if (ch === 'u') {
                            uffff = 0;
                            for (i = 0; i < 4; i += 1) {
                                hex = parseInt(next(), 16);
                                if (!isFinite(hex)) {
                                    break;
                                }
                                uffff = uffff * 16 + hex;
                            }
                            string += String.fromCharCode(uffff);
                        } else if (typeof escapee[ch] === 'string') {
                            string += escapee[ch];
                        } else {
                            break;
                        }
                    } else {
                        string += ch;
                    }
                }
            }
            error(\"Bad string\");
        },

        white = function () {

            while (ch && ch <= ' ') {
                next();
            }
        },

        word = function () {

            switch (ch) {
            case 't':
                next('t');
                next('r');
                next('u');
                next('e');
                return true;
            case 'f':
                next('f');
                next('a');
                next('l');
                next('s');
                next('e');
                return false;
            case 'n':
                next('n');
                next('u');
                next('l');
                next('l');
                return null;
            }
            error(\"Unexpected '\" + ch + \"'\");
        },

        value,  // Place holder for the value function.

        array = function () {

            var array = [];

            if (ch === '[') {
                next('[');
                white();
                if (ch === ']') {
                    next(']');
                    return array;   // empty array
                }
                while (ch) {
                    array.push(value());
                    white();
                    if (ch === ']') {
                        next(']');
                        return array;
                    }
                    next(',');
                    white();
                }
            }
            error(\"Bad array\");
        },

        object = function () {

            var key,
                object = {};

            if (ch === '{') {
                next('{');
                white();
                if (ch === '}') {
                    next('}');
                    return object;   // empty object
                }
                while (ch) {
                    key = string();
                    white();
                    next(':');
                    if (Object.hasOwnProperty.call(object, key)) {
                        error('Duplicate key \"' + key + '\"');
                    }
                    object[key] = value();
                    white();
                    if (ch === '}') {
                        next('}');
                        return object;
                    }
                    next(',');
                    white();
                }
            }
            error(\"Bad object\");
        };

    value = function () {

        white();
        switch (ch) {
        case '{':
            return object();
        case '[':
            return array();
        case '\"':
            return string();
        case '-':
            return number();
        default:
            return ch >= '0' && ch <= '9' ? number() : word();
        }
    };

    return function (source) {
        var result;

        text = source;
        at = 0;
        ch = ' ';
        result = value();
        white();
        if (ch) {
            error(\"Syntax error\");
        }
        return result;
    };
}());
  ")

