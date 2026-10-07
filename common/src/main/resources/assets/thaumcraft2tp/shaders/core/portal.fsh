#version 330
#moj_import <minecraft:dynamictransforms.glsl>
uniform sampler2D Sampler0;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;
void main() {
    // The original framebuffer used a ten-sided stencil aperture.
    vec2 p=(texCoord0-.5)*2.0;
    for(int i=0;i<10;i++) {
        float angle=(float(i)+.5)*6.28318530718/10.0;
        if(dot(p,vec2(cos(angle),sin(angle)))>cos(3.14159265359/10.0))discard;
    }
    fragColor=vec4(texture(Sampler0,texCoord0).rgb,1.0)*vertexColor*ColorModulator;
}
