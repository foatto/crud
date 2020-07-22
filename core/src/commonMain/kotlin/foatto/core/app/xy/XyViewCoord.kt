package foatto.core.app.xy

import foatto.core.app.xy.geom.XyRect

class XyViewCoord( var scale: Int, var x1: Int, var y1: Int, var x2: Int, var y2: Int ) {

    //--- конструкторы -----------------------------------------------------------------------------------------------------

    constructor() : this( 1, 0, 0, 1, 1 )
    constructor( view: XyViewCoord ) : this( view.scale, view.x1, view.y1, view.x2, view.y2 )
    constructor( aScale: Int, aRect: XyRect ) : this( aScale, aRect.x, aRect.y, aRect.x + aRect.width, aRect.y + aRect.height )

    //----------------------------------------------------------------------------------------------------------------------

    fun isEquals( view: XyViewCoord ) = scale == view.scale && x1 == view.x1 && y1 == view.y1 && x2 == view.x2 && y2 == view.y2

    //--- манипуляции всяческие

    fun moveRel( dx: Int, dy: Int ) {
        x1 += dx
        y1 += dy
        x2 += dx
        y2 += dy
    }

    //----------------------------------------------------------------------------------------------------------------------

    //    //--- расширить gView в koef раз
    //    public void expand( int koef ) {
    //        double dx = ( koef - 1 ) * ( getX2() - getX1() );
    //        double dy = ( koef - 1 ) * ( getY2() - getY1() );
    //        setBounds( getX1() - dx / 2, getY1() - dy / 2,
    //                   getX2() + dx / 2, getY2() + dy / 2 );
    //    }
    //
    //    //--- выравнивание (с расширением)
    //    public void var lign( double cellSize ) {
    //        setBounds( Math.floor( getX1() / cellSize ) * cellSize,
    //                   Math.floor( getY1() / cellSize ) * cellSize,
    //                   Math.ceil( getX2() / cellSize ) * cellSize,
    //                   Math.ceil( getY2() / cellSize ) * cellSize );
    //    }
    //
    //	//--- сложение двух прямоугольников
    //	public gView union( gView v2 ) {
    //
    //		double left = Math.min( v1.left, v2.left );
    //		double right = Math.max( v1.right, v2.right );
    //		double top = Math.min( v1.top, v2.top );
    //		double bottom = Math.max( v1.bottom, v2.bottom );
    //
    //		return new gView( v1.getScale(), left, bottom, right, top, v1.getBack() );
    //	}

}
