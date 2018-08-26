/*
 * Copyright (C) 2018 INAOS GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.inaos.jam.agent;

import net.bytebuddy.jar.asm.Handle;
import net.bytebuddy.jar.asm.Label;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

abstract class CheckSumVisitor extends MethodVisitor {

    private static final MessageDigest MESSAGE_DIGEST;

    private static final Charset CHARSET;

    static {
        MessageDigest md;
        Charset charset;
        try {
            md = MessageDigest.getInstance("MD5");
            charset = Charset.forName("utf-8");
        } catch (NoSuchAlgorithmException e) {
            md = null;
            charset = null;
        }
        MESSAGE_DIGEST = md;
        CHARSET = charset;
    }

    private final StringBuilder sb;

    CheckSumVisitor() {
        super(Opcodes.ASM6);
        this.sb = new StringBuilder();
    }

    static boolean isMd5Available() {
        return MESSAGE_DIGEST != null;
    }

    @Override
    public void visitInsn(int opcode) {
        sb.append(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        sb.append(opcode).append(opcode);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        sb.append(opcode).append(var);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        sb.append(opcode).append(type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        sb.append(opcode).append(owner).append(name).append(desc);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        sb.append(opcode).append(owner).append(name).append(desc).append(itf);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        sb.append(name).append(desc);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        sb.append(opcode);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        sb.append(cst);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        sb.append(var).append(increment);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        sb.append(min).append(max);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        for (int key : keys) {
            sb.append(key);
        }
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        sb.append(desc).append(dims);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        sb.append(type);
    }

    @Override
    public void visitEnd() {
        if (MESSAGE_DIGEST != null) {
            byte[] digest = MESSAGE_DIGEST.digest(sb.toString().getBytes(CHARSET));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            onChecksum(hex.toString());
        }
    }

    abstract void onChecksum(String checksum);
}
