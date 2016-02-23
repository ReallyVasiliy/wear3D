uniform mat4 uMVPMatrix;
attribute vec4 vPosition;
attribute vec4 vColor;
varying vec4 _vColor;

void main() {
    _vColor = vColor;
    gl_Position = uMVPMatrix * vPosition;
}
