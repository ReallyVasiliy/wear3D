precision mediump float;
varying vec4 _vColor;

void main() {
	gl_FragColor = _vColor * vec4(0.6, 0.6, 0.6, 1.0);
}