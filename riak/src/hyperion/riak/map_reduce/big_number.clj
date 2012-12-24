(ns hyperion.riak.map-reduce.big-number)

(def big-number-js
  "
var BigNumber = (function () {
    var P = Big.prototype,
        isValid = /^-?\\d+(?:\\.\\d+)?(?:e[+-]?\\d+)?$/i;

    function Big( n ) {
        var i, j, nL,
            x = this;
        if ( !(x instanceof Big) ) {
            return new Big( n )
        }
        if ( n instanceof Big ) {
            x['s'] = n['s'];
            x['e'] = n['e'];
            x['c'] = n['c'].slice();
            return
        }
        if ( n === 0 && 1 / n < 0 ) {
            n = '-0'
        } else if ( !isValid.test(n += '') ) {
            throw NaN
        }
        x['s'] = n.charAt(0) == '-' ? ( n = n.slice(1), -1 ) : 1;

        if ( ( i = n.indexOf('.') ) > -1 ) {
            n = n.replace( '.', '' )
        }

        if ( ( j = n.search(/e/i) ) > 0 ) {

            if ( i < 0 ) {
                i = j
            }
            i += +n.slice( j + 1 );
            n = n.substring( 0, j )

        } else if ( i < 0 ) {

            i = n.length
        }

        for ( j = 0; n.charAt(j) == '0'; j++ ) {
        }

        if ( j == ( nL = n.length ) ) {

            x['c'] = [ x['e'] = 0 ]
        } else {

            for ( ; n.charAt(--nL) == '0'; ) {
            }

            x['e'] = i - j - 1;
            x['c'] = [];

            for ( i = 0; j <= nL; x['c'][i++] = +n.charAt(j++) ) {
            }
        }
    }

    P['cmp'] = function ( y ) {
        var xNeg,
            x = this,
            xc = x['c'],
            yc = ( y = new Big( y ) )['c'],
            i = x['s'],
            j = y['s'],
            k = x['e'],
            l = y['e'];

        if ( !xc[0] || !yc[0] ) {
            return !xc[0] ? !yc[0] ? 0 : -j : i
        }

        if ( i != j ) {
            return i
        }
        xNeg = i < 0;

        if ( k != l ) {
            return k > l ^ xNeg ? 1 : -1
        }

        for ( i = -1,
              j = ( k = xc.length ) < ( l = yc.length ) ? k : l;
              ++i < j; ) {

            if ( xc[i] != yc[i] ) {
                return xc[i] > yc[i] ^ xNeg ? 1 : -1
            }
        }

        return k == l ? 0 : k > l ^ xNeg ? 1 : -1
    };

    return Big;

}());
  ")
